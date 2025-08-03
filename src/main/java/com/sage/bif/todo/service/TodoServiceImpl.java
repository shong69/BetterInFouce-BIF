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
import com.sage.bif.todo.entity.SubTodo;
import com.sage.bif.todo.entity.Todo;
import com.sage.bif.todo.entity.enums.RepeatDays;
import com.sage.bif.todo.entity.enums.RepeatFrequency;
import com.sage.bif.todo.entity.enums.TodoTypes;
import com.sage.bif.todo.exception.*;
import com.sage.bif.todo.repository.SubTodoRepository;
import com.sage.bif.todo.repository.TodoRepository;
import com.sage.bif.user.entity.Bif;
import com.sage.bif.user.repository.BifRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final BifRepository bifRepository;
    private final TodoRepository todoRepository;
    private final SubTodoRepository subTodoRepository;

    private final RoutineCompletionService routineCompletionService;

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

        return todoList.stream().map(TodoListResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TodoUpdatePageResponse getTodoDetail(Long bifId, Long todoId) {
        Todo todo = todoRepository.findTodoDetailsById(bifId, todoId)
                .orElseThrow(() -> new TodoNotFoundException(todoId));

        validateUserPermission(todo, bifId);

        return TodoUpdatePageResponse.from(todo);
    }

    @Override
    @Transactional
    public TodoListResponse updateTodo(Long bifId, Long todoId, TodoUpdateRequest request) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoNotFoundException(todoId));

        validateUserPermission(todo, bifId);

        updateTodoBasicInfo(todo, request);

        updateSubTodos(todo, request.getSubTodos());

        return TodoListResponse.from(todo);
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
    public TodoListResponse completeTodo(Long bifId, Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoNotFoundException(todoId));

        validateUserPermission(todo, bifId);

        if (todo.getType() == TodoTypes.TASK) {
            validateAllSubTodosCompleted(todo);

            todo.setIsCompleted(true);
            todo.setCompletedAt(LocalDateTime.now());

        } else if (todo.getType() == TodoTypes.ROUTINE) {
            routineCompletionService.completeRoutine(bifId, todoId, LocalDate.now());
        }

        return TodoListResponse.from(todo);
    }

    @Override
    @Transactional
    public TodoListResponse uncompleteTodo(Long bifId, Long todoId) {
        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new TodoNotFoundException(todoId));

        validateUserPermission(todo, bifId);

        if (todo.getType() == TodoTypes.TASK) {
            todo.setIsCompleted(false);
            todo.setCompletedAt(null);
        } else if (todo.getType() == TodoTypes.ROUTINE) {
            routineCompletionService.uncompleteRoutine(bifId, todoId, LocalDate.now());
        }

        return TodoListResponse.from(todo);
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

    private void validateAllSubTodosCompleted(Todo todo) {
        if (todo.getSubTodos() != null && !todo.getSubTodos().isEmpty()) {
            List<SubTodo> activeSubTodos = todo.getSubTodos().stream()
                    .filter(subTodo -> !subTodo.getIsDeleted())
                    .toList();

            if (!activeSubTodos.isEmpty()) {
                long incompletedCount = activeSubTodos.stream()
                        .filter(subTodo -> !subTodo.getIsCompleted())
                        .count();

                if (incompletedCount > 0) {
                    throw new TodoCompletionException(todo.getTodoId(), (int) incompletedCount);
                }
            }
        }
    }

    private void updateTodoBasicInfo(Todo todo, TodoUpdateRequest request) {
        todo.setTitle(request.getTitle());
        todo.setType(request.getType());
        todo.setRepeatFrequency(request.getRepeatFrequency());
        todo.setRepeatDays(request.getRepeatDays());
        todo.setDueDate(request.getDueDate());
        todo.setDueTime(request.getDueTime());
        todo.setNotificationEnabled(request.getNotificationEnabled());
        todo.setNotificationTime(request.getNotificationTime());
    }

    private void updateSubTodos(Todo todo, List<SubTodoUpdateRequest> requestSubTodos) {
        List<SubTodo> existingSubTodos = todo.getSubTodos() != null ?
                todo.getSubTodos().stream()
                        .filter(subTodo -> !subTodo.getIsDeleted())
                        .toList() :
                Collections.emptyList();

        if (requestSubTodos == null || requestSubTodos.isEmpty()) {
            existingSubTodos.forEach(subTodo -> subTodo.setIsDeleted(true));
            return;
        }

        Map<Long, SubTodo> existingSubTodoMap = existingSubTodos.stream()
                .collect(Collectors.toMap(SubTodo::getSubTodoId, Function.identity()));

        Set<Long> requestSubTodoIds = requestSubTodos.stream()
                .map(SubTodoUpdateRequest::getSubTodoId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<SubTodo> newSubTodos = new ArrayList<>();

        for (SubTodoUpdateRequest requestSubTodo : requestSubTodos) {
            Long subTodoId = requestSubTodo.getSubTodoId();

            if (subTodoId != null && existingSubTodoMap.containsKey(subTodoId)) {
                SubTodo existingSubTodo = existingSubTodoMap.get(subTodoId);
                existingSubTodo.setTitle(requestSubTodo.getTitle());
                existingSubTodo.setSortOrder(requestSubTodo.getSortOrder());
            } else {
                SubTodo newSubTodo = SubTodo.builder()
                        .todo(todo)
                        .title(requestSubTodo.getTitle())
                        .sortOrder(requestSubTodo.getSortOrder())
                        .isCompleted(false)
                        .isDeleted(false)
                        .build();
                newSubTodos.add(newSubTodo);
            }
        }

        existingSubTodos.stream()
                .filter(subTodo -> !requestSubTodoIds.contains(subTodo.getSubTodoId()))
                .forEach(subTodo -> subTodo.setIsDeleted(true));

        if (!newSubTodos.isEmpty()) {
            subTodoRepository.saveAll(newSubTodos);
            if (todo.getSubTodos() == null) {
                todo.setSubTodos(new ArrayList<>());
            }
            todo.getSubTodos().addAll(newSubTodos);
        }
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
        Todo.TodoBuilder builder = Todo.builder()
                .bifUser(bif)
                .userInput(userInput)
                .title(parsedData.getTitle() != null ? parsedData.getTitle() : "제목 없음")
                .type(safeParseEnum(parsedData.getType(), TodoTypes.class, TodoTypes.TASK))
                .dueDate(parseDate(parsedData.getDate()))
                .dueTime(parseTime(parsedData.getTime()));

        if (parsedData.getRepeatFrequency() != null) {
            RepeatFrequency frequency = safeParseEnum(parsedData.getRepeatFrequency(), RepeatFrequency.class, null);
            builder.repeatFrequency(frequency);
        }

        if (parsedData.getRepeatDays() != null && !parsedData.getRepeatDays().isEmpty()) {
            List<RepeatDays> repeatDays = parsedData.getRepeatDays().stream()
                    .map(day -> safeParseEnum(day, RepeatDays.class, null))
                    .filter(Objects::nonNull)
                    .toList();

            if (!repeatDays.isEmpty()) {
                builder.repeatDays(repeatDays);
            }
        }

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
                int sortOrder = hasOrder ? i + 1 : 0;

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

}