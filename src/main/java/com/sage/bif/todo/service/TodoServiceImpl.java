package com.sage.bif.todo.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.bif.common.client.ai.AiServiceClient;
import com.sage.bif.common.client.ai.AiSettings;
import com.sage.bif.common.client.ai.dto.AiRequest;
import com.sage.bif.common.client.ai.dto.AiResponse;
import com.sage.bif.todo.dto.request.AiTodoCreateRequest;
import com.sage.bif.todo.dto.request.SubTodoUpdateRequest;
import com.sage.bif.todo.dto.request.TodoUpdateRequest;
import com.sage.bif.todo.dto.response.AiTaskParseResponse;
import com.sage.bif.todo.dto.response.TodoListResponse;
import com.sage.bif.todo.dto.response.TodoUpdatePageResponse;
import com.sage.bif.todo.entity.RoutineCompletion;
import com.sage.bif.todo.entity.SubTodo;
import com.sage.bif.todo.entity.SubTodoCompletion;
import com.sage.bif.todo.entity.Todo;
import com.sage.bif.todo.entity.enums.RepeatDays;
import com.sage.bif.todo.entity.enums.RepeatFrequency;
import com.sage.bif.todo.entity.enums.TodoTypes;
import com.sage.bif.todo.exception.*;
import com.sage.bif.todo.repository.RoutineCompletionRepository;
import com.sage.bif.todo.repository.SubTodoCompletionRepository;
import com.sage.bif.todo.repository.SubTodoRepository;
import com.sage.bif.todo.repository.TodoRepository;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.repository.BifRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private static final String TIMEZONE_ASIA_SEOUL = "Asia/Seoul";
    private static final int DEFAULT_SORT_ORDER = 0;
    private static final int SORT_ORDER_BASE = 1;
    private static final int MAX_SUBTODOS = 5;

    private final BifRepository bifRepository;
    private final TodoRepository todoRepository;
    private final SubTodoRepository subTodoRepository;
    private final RoutineCompletionRepository routineCompletionRepository;
    private final SubTodoCompletionRepository subTodoCompletionRepository;

    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TodoListResponse createTodoByAi(Long bifId, AiTodoCreateRequest request) {
        Bif bif = bifRepository.findById(bifId).orElseThrow(() -> new UserNotFoundException(bifId));

        try {
            AiRequest aiRequest = new AiRequest(request.getUserInput());
            AiResponse aiResponse = aiServiceClient.generate(aiRequest, AiSettings.getTodoCreationWithCurrentDate());

            AiTaskParseResponse parsedData = parseAiResponse(aiResponse);

            Todo newTodo = createTodoFromParsedData(bif, request.getUserInput(), parsedData);

            List<SubTodo> subTodos = createSubTodosFromParsedData(newTodo, parsedData);

            todoRepository.save(newTodo);
            if (!subTodos.isEmpty()) {
                subTodoRepository.saveAll(subTodos);
                newTodo.setSubTodos(subTodos);
            }

            return TodoListResponse.from(newTodo);

        } catch (Exception e) {
            throw new AiResponseParsingException("AI 할일 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoListResponse> getTodoList(Long bifId, LocalDate date) {
        validateUserExists(bifId);

        List<Todo> todoList = todoRepository.findTodoWithSubTodosByBifIdAndDate(bifId, date);

        return todoList.stream()
                .filter(todo -> {
                    if (todo.getType() == TodoTypes.ROUTINE) {
                        return isValidDayForRoutine(todo, date);
                    }
                    return true;
                })
                .map(todo -> {
                    if (todo.getType() == TodoTypes.ROUTINE) {
                        boolean isCompletedToday = isRoutineCompletedToday(todo, date);
                        return TodoListResponse.from(todo, isCompletedToday);
                    } else {
                        return TodoListResponse.from(todo);
                    }
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TodoUpdatePageResponse getTodoDetail(Long bifId, Long todoId) {
        LocalDate viewDate = LocalDate.now(ZoneId.of(TIMEZONE_ASIA_SEOUL));
        return getTodoDetailInternal(bifId, todoId, viewDate);
    }

    @Override
    @Transactional(readOnly = true)
    public TodoUpdatePageResponse getTodoDetail(Long bifId, Long todoId, LocalDate viewDate) {
        return getTodoDetailInternal(bifId, todoId, viewDate);
    }

    private TodoUpdatePageResponse getTodoDetailInternal(Long bifId, Long todoId, LocalDate viewDate) {
        Todo todo = todoRepository.findTodoDetailsById(bifId, todoId)
                .orElseThrow(() -> new TodoNotFoundException(todoId));

        validateUserPermission(todo, bifId);

        if (todo.getType() == TodoTypes.ROUTINE) {
            boolean isCompletedOnDate = isRoutineCompletedToday(todo, viewDate);
            List<SubTodoCompletion> subTodoCompletions = subTodoCompletionRepository
                    .findByTodoIdAndCompletionDate(todoId, viewDate);
            return TodoUpdatePageResponse.from(todo, isCompletedOnDate, subTodoCompletions);
        }

        return TodoUpdatePageResponse.from(todo);
    }

    @Override
    @Transactional
    public TodoUpdatePageResponse updateTodo(Long bifId, Long todoId, TodoUpdateRequest request) {
        if (request.getSubTodos() != null && request.getSubTodos().size() > MAX_SUBTODOS) {
            throw new SubTodoCountInsufficientException("세부 할일은 최대 " + MAX_SUBTODOS + "개까지 가능합니다.");
        }

        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoNotFoundException(todoId));

        validateUserPermission(todo, bifId);

        boolean timeChanged = hasTimeChanged(todo, request);

        updateTodoBasicInfo(todo, request);
        updateSubTodos(todo, request.getSubTodos());

        if (timeChanged) {
            todo.setLastNotificationSentAt(null);
        }

        Todo savedTodo = todoRepository.save(todo);

        if (savedTodo.getType() == TodoTypes.ROUTINE) {
            LocalDate today = LocalDate.now(ZoneId.of(TIMEZONE_ASIA_SEOUL));
            List<SubTodoCompletion> subTodoCompletions = subTodoCompletionRepository
                    .findByTodoIdAndCompletionDate(savedTodo.getTodoId(), today);
            boolean isCompletedToday = isRoutineCompletedToday(savedTodo, today);
            return TodoUpdatePageResponse.from(savedTodo, isCompletedToday, subTodoCompletions);
        }

        log.debug("Eagerly loaded subTodos size: {}", savedTodo.getSubTodos().size());
        if (savedTodo.getRepeatDays() != null) {
            log.debug("Eagerly loaded repeatDays size: {}", savedTodo.getRepeatDays().size());
        }

        return TodoUpdatePageResponse.from(savedTodo);
    }

    @Override
    @Transactional
    public boolean deleteTodo(Long bifId, Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoNotFoundException(todoId));

        validateUserPermission(todo, bifId);

        todo.setIsDeleted(true);

        if (todo.getSubTodos() != null) {
            todo.getSubTodos().forEach(subTodo -> subTodo.setIsDeleted(true));
        }

        return true;
    }

    @Override
    @Transactional
    public TodoListResponse updateTodoCompletion(Long bifId, Long todoId, LocalDate completionDate, boolean isCompleted) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoNotFoundException(todoId));
        validateUserPermission(todo, bifId);

        if (isCompleted) {
            if (todo.getType() == TodoTypes.ROUTINE) {
                return handleRoutineCompletion(todo, todoId, completionDate);
            } else {
                return handleTaskCompletion(todo, todoId);
            }
        } else {
            if (todo.getType() == TodoTypes.ROUTINE) {
                routineCompletionRepository.deleteCompletion(todoId, completionDate);
                return TodoListResponse.from(todo, false);
            } else {
                todo.setIsCompleted(false);
                todo.setCompletedAt(null);
                todoRepository.save(todo);
                return TodoListResponse.from(todo, false);
            }
        }
    }

    @Override
    @Transactional
    public void updateCurrentStep(Long bifId, Long todoId, int newStep) {
        Todo todo = todoRepository.findTodoDetailsById(bifId, todoId)
                .orElseThrow(() -> new TodoNotFoundException(todoId));
        validateUserPermission(todo, bifId);

        boolean hasOrder = todo.getSubTodos() != null &&
                !todo.getSubTodos().isEmpty() &&
                todo.getSubTodos().stream().anyMatch(st -> st.getSortOrder() != null && st.getSortOrder() > 0);

        if (hasOrder) {
            if (todo.getType() == TodoTypes.ROUTINE) {
                updateRoutineSubTodoSteps(todo, newStep);
            } else {
                updateTaskSubTodoSteps(todo, newStep);
            }
        }

        todo.setCurrentStep(newStep);
        todoRepository.save(todo);
    }

    private void updateTaskSubTodoSteps(Todo todo, int newStep) {
        for (SubTodo subTodo : todo.getSubTodos()) {
            if (subTodo.getSortOrder() == null) {
                continue;
            }

            boolean shouldBeCompleted = subTodo.getSortOrder() <= newStep;
            if (shouldBeCompleted && Boolean.FALSE.equals(subTodo.getIsCompleted())) {
                subTodo.setIsCompleted(true);
                subTodo.setCompletedAt(LocalDateTime.now(ZoneId.of(TIMEZONE_ASIA_SEOUL)));
            } else if (!shouldBeCompleted && Boolean.TRUE.equals(subTodo.getIsCompleted())) {
                subTodo.setIsCompleted(false);
                subTodo.setCompletedAt(null);
            }
        }
    }

    private void updateRoutineSubTodoSteps(Todo todo, int newStep) {
        LocalDate today = LocalDate.now(ZoneId.of(TIMEZONE_ASIA_SEOUL));
        if (newStep < todo.getSubTodos().size() - 1) {
            todo.getSubTodos().forEach(subTodo -> {
                if (subTodo.getSortOrder() != null && subTodo.getSortOrder() > newStep) {
                    subTodoCompletionRepository.deleteBySubTodo_SubTodoIdAndCompletionDate(subTodo.getSubTodoId(), today);
                }
            });
            routineCompletionRepository.deleteCompletion(todo.getTodoId(), today);
        }
    }

    private void validateUserExists(Long bifId) {
        if (!bifRepository.existsById(bifId)) {
            throw new UserNotFoundException(bifId);
        }
    }

    private void validateUserPermission(Todo todo, Long bifId) {
        if (!todo.getBifUser().getBifId().equals(bifId)) {
            throw new UnauthorizedTodoAccessException(todo.getTodoId());
        }
    }

    private void updateTodoBasicInfo(Todo todo, TodoUpdateRequest request) {
        todo.setTitle(request.getTitle());
        todo.setType(request.getType());

        if (request.getType() == TodoTypes.ROUTINE) {
            if (request.getRepeatFrequency() == null) {
                todo.setRepeatFrequency(RepeatFrequency.DAILY);
            } else {
                todo.setRepeatFrequency(request.getRepeatFrequency());
            }

            if (request.getRepeatDays() == null || request.getRepeatDays().isEmpty()) {
                todo.setRepeatDays(new ArrayList<>(List.of(RepeatDays.MONDAY, RepeatDays.TUESDAY, RepeatDays.WEDNESDAY,
                        RepeatDays.THURSDAY, RepeatDays.FRIDAY, RepeatDays.SATURDAY, RepeatDays.SUNDAY)));
            } else {
                todo.setRepeatDays(request.getRepeatDays());
            }
        } else {
            todo.setRepeatFrequency(request.getRepeatFrequency());
            todo.setRepeatDays(request.getRepeatDays());
        }

        todo.setDueDate(request.getDueDate());
        todo.setDueTime(request.getDueTime());
        todo.setNotificationEnabled(request.getNotificationEnabled());
        todo.setNotificationTime(request.getNotificationTime() != null ? request.getNotificationTime() : 0);
    }

    private void updateSubTodos(Todo todo, List<SubTodoUpdateRequest> requestSubTodos) {
        if (requestSubTodos == null) {
            requestSubTodos = Collections.emptyList();
        }

        List<SubTodo> existingSubTodos = getExistingSubTodos(todo);
        Map<Long, SubTodo> existingSubTodoMap = createExistingSubTodoMap(existingSubTodos);

        List<SubTodo> finalSubTodos = upsertAndGetFinalList(todo, requestSubTodos, existingSubTodoMap);
        markRemovedAsDeleted(existingSubTodos, getRequestSubTodoIds(requestSubTodos));

        todo.getSubTodos().clear();
        todo.getSubTodos().addAll(finalSubTodos);
    }

    private List<SubTodo> upsertAndGetFinalList(Todo todo, List<SubTodoUpdateRequest> requestSubTodos, Map<Long, SubTodo> existingSubTodoMap) {
        boolean isSequenceMode = isSequenceMode(requestSubTodos);
        List<SubTodo> subTodosToSave = new ArrayList<>();
        for (SubTodoUpdateRequest requestSubTodo : requestSubTodos) {
            subTodosToSave.add(upsertSingleSubTodo(todo, requestSubTodo, existingSubTodoMap, isSequenceMode));
        }
        return subTodosToSave;
    }

    private SubTodo upsertSingleSubTodo(Todo todo, SubTodoUpdateRequest requestSubTodo, Map<Long, SubTodo> existingSubTodoMap, boolean isSequenceMode) {
        Long subTodoId = requestSubTodo.getSubTodoId();
        if (subTodoId != null && existingSubTodoMap.containsKey(subTodoId)) {
            SubTodo existingSubTodo = existingSubTodoMap.get(subTodoId);
            updateExistingSubTodo(existingSubTodo, requestSubTodo, isSequenceMode);
            existingSubTodo.setIsDeleted(false);
            return existingSubTodo;
        } else {
            return createNewSubTodo(todo, requestSubTodo, isSequenceMode);
        }
    }

    private void markRemovedAsDeleted(List<SubTodo> existingSubTodos, Set<Long> requestSubTodoIds) {
        for (SubTodo subTodo : existingSubTodos) {
            if (!requestSubTodoIds.contains(subTodo.getSubTodoId())) {
                subTodo.setIsDeleted(true);
            }
        }
    }

    private List<SubTodo> getExistingSubTodos(Todo todo) {
        return todo.getSubTodos() != null ?
                new ArrayList<>(todo.getSubTodos().stream()
                        .filter(subTodo -> !subTodo.getIsDeleted())
                        .toList()) :
                new ArrayList<>();
    }

    private boolean isSequenceMode(List<SubTodoUpdateRequest> requestSubTodos) {
        return requestSubTodos.stream()
                .anyMatch(s -> s.getSortOrder() != null && s.getSortOrder() > DEFAULT_SORT_ORDER);
    }

    private Map<Long, SubTodo> createExistingSubTodoMap(List<SubTodo> existingSubTodos) {
        return existingSubTodos.stream()
                .collect(Collectors.toMap(SubTodo::getSubTodoId, Function.identity()));
    }

    private Set<Long> getRequestSubTodoIds(List<SubTodoUpdateRequest> requestSubTodos) {
        return requestSubTodos.stream()
                .map(SubTodoUpdateRequest::getSubTodoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void updateExistingSubTodo(SubTodo existingSubTodo, SubTodoUpdateRequest requestSubTodo, boolean isSequenceMode) {
        existingSubTodo.setTitle(requestSubTodo.getTitle());
        existingSubTodo.setSortOrder(isSequenceMode ? requestSubTodo.getSortOrder() : DEFAULT_SORT_ORDER);
    }

    private SubTodo createNewSubTodo(Todo todo, SubTodoUpdateRequest requestSubTodo, boolean isSequenceMode) {
        return SubTodo.builder()
                .todo(todo)
                .title(requestSubTodo.getTitle())
                .sortOrder(isSequenceMode ? requestSubTodo.getSortOrder() : DEFAULT_SORT_ORDER)
                .isCompleted(false)
                .isDeleted(false)
                .build();
    }

    private AiTaskParseResponse parseAiResponse(AiResponse aiResponse) {
        try {
            String jsonContent = aiResponse.getContent();
            jsonContent = cleanJsonResponse(jsonContent);

            return objectMapper.readValue(jsonContent, AiTaskParseResponse.class);
        } catch (Exception e) {
            throw new AiResponseParsingException("AI 응답 파싱 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return response;
        }

        response = response.trim();

        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }

        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }

        return response.trim();
    }

    private Todo createTodoFromParsedData(Bif bif, String userInput, AiTaskParseResponse parsedData) {
        TodoTypes todoType = safeParseEnum(parsedData.getType(), TodoTypes.class, TodoTypes.TASK);

        Todo.TodoBuilder builder = Todo.builder()
                .bifUser(bif)
                .userInput(userInput)
                .title(parsedData.getTitle() != null ? parsedData.getTitle() : "제목 없음")
                .type(todoType)
                .dueDate(parseDate(parsedData.getDate()))
                .dueTime(parseTime(parsedData.getTime()));

        if (parsedData.getRepeatFrequency() != null) {
            RepeatFrequency frequency = safeParseEnum(parsedData.getRepeatFrequency(), RepeatFrequency.class, null);
            builder.repeatFrequency(frequency);
        }

        setRepeatDaysForBuilder(builder, parsedData, todoType);

        return builder.build();
    }

    private <T extends Enum<T>> T safeParseEnum(String value, Class<T> enumClass, T defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private LocalTime parseTime(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return null;
        }
        return LocalTime.parse(timeString, DateTimeFormatter.ISO_LOCAL_TIME);
    }

    private List<SubTodo> createSubTodosFromParsedData(Todo todo, AiTaskParseResponse parsedData) {
        if (parsedData.getSubTasks() == null || parsedData.getSubTasks().isEmpty()) {
            return Collections.emptyList();
        }

        List<SubTodo> subTodos = new ArrayList<>();
        boolean hasOrder = parsedData.isHasOrder();

        for (int i = 0; i < parsedData.getSubTasks().size(); i++) {
            String taskTitle = parsedData.getSubTasks().get(i);
            if (taskTitle != null && !taskTitle.trim().isEmpty()) {
                int sortOrder = hasOrder ? i + SORT_ORDER_BASE : DEFAULT_SORT_ORDER;

                SubTodo subTodo = SubTodo.builder()
                        .todo(todo)
                        .title(taskTitle.trim())
                        .sortOrder(sortOrder)
                        .build();

                subTodos.add(subTodo);
            }
        }

        return subTodos;
    }

    private boolean isRoutineCompletedToday(Todo todo, LocalDate date) {
        if (todo.getType() != TodoTypes.ROUTINE) {
            return false;
        }

        boolean isPresent = routineCompletionRepository
                .findByTodo_TodoIdAndCompletionDate(todo.getTodoId(), date)
                .isPresent();
        log.info("[BIF_INTERNAL_LOG] isRoutineCompletedToday for todoId: {} on date: {} returns: {}", todo.getTodoId(), date, isPresent);
        return isPresent;
    }

    private boolean hasTimeChanged(Todo existingTodo, TodoUpdateRequest request) {
        return !Objects.equals(existingTodo.getDueDate(), request.getDueDate()) ||
                !Objects.equals(existingTodo.getDueTime(), request.getDueTime()) ||
                !Objects.equals(existingTodo.getNotificationTime(), request.getNotificationTime()) ||
                !Objects.equals(existingTodo.getNotificationEnabled(), request.getNotificationEnabled());
    }

    private boolean isValidDayForRoutine(Todo todo, LocalDate date) {
        if (todo.getRepeatDays() == null || todo.getRepeatDays().isEmpty()) {
            return false;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        RepeatDays targetDay = convertToRepeatDay(dayOfWeek);

        return todo.getRepeatDays().contains(targetDay);
    }

    private RepeatDays convertToRepeatDay(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> RepeatDays.MONDAY;
            case TUESDAY -> RepeatDays.TUESDAY;
            case WEDNESDAY -> RepeatDays.WEDNESDAY;
            case THURSDAY -> RepeatDays.THURSDAY;
            case FRIDAY -> RepeatDays.FRIDAY;
            case SATURDAY -> RepeatDays.SATURDAY;
            case SUNDAY -> RepeatDays.SUNDAY;
        };
    }

    private TodoListResponse handleRoutineCompletion(Todo todo, Long todoId, LocalDate completionDate) {
        routineCompletionRepository.findByTodo_TodoIdAndCompletionDate(todoId, completionDate)
                .ifPresentOrElse(
                        rc -> log.info("[BIF_INTERNAL_LOG] Routine completion for todoId: {} on date: {} already exists.", todoId, completionDate),
                        () -> {
                            RoutineCompletion newCompletion = RoutineCompletion.builder()
                                    .todo(todo)
                                    .completionDate(completionDate)
                                    .build();
                            routineCompletionRepository.save(newCompletion);
                            log.info("[BIF_INTERNAL_LOG] Inserted routine completion for todoId: {} on date: {}", todoId, completionDate);

                            if (todo.getSubTodos() != null && !todo.getSubTodos().isEmpty()) {
                                todo.getSubTodos().forEach(subTodo ->
                                        subTodoCompletionRepository.insertIgnoreCompletion(subTodo.getSubTodoId(), completionDate)
                                );
                            }
                        }
                );
        return TodoListResponse.from(todo, true);
    }

    private TodoListResponse handleTaskCompletion(Todo todo, Long todoId) {
        boolean hasOrder = todo.getSubTodos() != null &&
                !todo.getSubTodos().isEmpty() &&
                todo.getSubTodos().stream().allMatch(st -> st.getSortOrder() != null && st.getSortOrder() > 0);

        if (hasOrder) {
            todo.getSubTodos().forEach(subTodo -> {
                if (Boolean.FALSE.equals(subTodo.getIsDeleted())) {
                    subTodo.setIsCompleted(true);
                    subTodo.setCompletedAt(LocalDateTime.now(ZoneId.of(TIMEZONE_ASIA_SEOUL)));
                }
            });
        } else {
            if (todo.getSubTodos() != null && !todo.getSubTodos().isEmpty()) {
                long incompleteSubTodos = todo.getSubTodos().stream()
                        .filter(subTodo -> !subTodo.getIsCompleted() && !subTodo.getIsDeleted())
                        .count();
                if (incompleteSubTodos > 0) {
                    throw new TodoCompletionException(todoId, (int) incompleteSubTodos);
                }
            }
        }

        todo.setIsCompleted(true);
        todo.setCompletedAt(LocalDateTime.now(ZoneId.of(TIMEZONE_ASIA_SEOUL)));
        todo.setLastNotificationSentAt(null);

        Todo savedTodo = todoRepository.save(todo);

        return TodoListResponse.from(savedTodo, true);
    }

    private void setRepeatDaysForBuilder(Todo.TodoBuilder builder, AiTaskParseResponse parsedData, TodoTypes todoType) {
        if (parsedData.getRepeatDays() != null && !parsedData.getRepeatDays().isEmpty()) {
            List<RepeatDays> repeatDays = parsedData.getRepeatDays().stream()
                    .map(day -> safeParseEnum(day, RepeatDays.class, null))
                    .filter(Objects::nonNull)
                    .toList();

            if (!repeatDays.isEmpty()) {
                builder.repeatDays(repeatDays);
            } else if (todoType == TodoTypes.ROUTINE) {
                builder.repeatDays(List.of(RepeatDays.MONDAY, RepeatDays.TUESDAY, RepeatDays.WEDNESDAY,
                        RepeatDays.THURSDAY, RepeatDays.FRIDAY, RepeatDays.SATURDAY, RepeatDays.SUNDAY));
            }
        } else if (todoType == TodoTypes.ROUTINE) {
            builder.repeatDays(List.of(RepeatDays.MONDAY, RepeatDays.TUESDAY, RepeatDays.WEDNESDAY,
                    RepeatDays.THURSDAY, RepeatDays.FRIDAY, RepeatDays.SATURDAY, RepeatDays.SUNDAY));
        }
    }

}
