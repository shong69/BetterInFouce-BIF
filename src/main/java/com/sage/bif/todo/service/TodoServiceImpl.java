package com.sage.bif.todo.service;


import com.sage.bif.todo.dto.request.AiTodoCreateRequest;
import com.sage.bif.todo.dto.request.SubTodoUpdateRequest;
import com.sage.bif.todo.dto.request.TodoUpdateRequest;
import com.sage.bif.todo.dto.response.TodoListResponse;
import com.sage.bif.todo.dto.response.TodoUpdatePageResponse;
import com.sage.bif.todo.entity.SubTodo;
import com.sage.bif.todo.entity.Todo;
import com.sage.bif.todo.entity.enums.TodoTypes;
import com.sage.bif.todo.repository.SubTodoRepository;
import com.sage.bif.todo.repository.TodoRepository;
import com.sage.bif.user.entity.User;
import com.sage.bif.user.repository.UserRepository;
import com.sage.bif.todo.exception.TodoNotFoundException;
import com.sage.bif.todo.exception.UnauthorizedTodoAccessException;
import com.sage.bif.todo.exception.TodoCompletionException;
import com.sage.bif.todo.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final UserRepository userRepository;
    private final TodoRepository todoRepository;
    private final SubTodoRepository subTodoRepository;

    private final SubTodoService subTodoService;
    private final RoutineCompletionService routineCompletionService;

    @Override
    @Transactional
    public TodoListResponse createTodoByAi(Long bifId, AiTodoCreateRequest request) {

        User user = userRepository.findById(bifId).orElseThrow(() -> new UserNotFoundException(bifId));

        Todo newTodo = Todo.builder()
                .bifId(user)
                .userInput(request.getUserInput())
                .title("")
                .type(TodoTypes.TASK)
                .repeatFrequency(null)
                .repeatDays(null)
                .dueDate(null)
                .dueTime(null)
                .build();

        Todo savedTodo = todoRepository.save(newTodo);

        return TodoListResponse.from(savedTodo);

    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoListResponse> getTodoList(Long bifId, LocalDate date) {

        List<Todo> todoList = todoRepository.findTodoWithSubTodosByBifIdAndDate(bifId, date);

        return todoList.stream().map(TodoListResponse::from).collect(Collectors.toList());

    }

    @Override
    @Transactional(readOnly = true)
    public TodoUpdatePageResponse getTodoUpdatePageList(Long bifId, Long todoId) {

        Todo todo = todoRepository.findTodoDetailsById(bifId, todoId).orElseThrow(() -> new TodoNotFoundException(todoId));

        validateUserPermission(todo, bifId);

        return TodoUpdatePageResponse.from(todo);

    }

    @Override
    @Transactional
    public TodoListResponse updateTodo(Long bifId, Long todoId, TodoUpdateRequest request) {

        Todo todo = todoRepository.findById(todoId).orElseThrow(() -> new TodoNotFoundException(todoId));

        validateUserPermission(todo, bifId);

        todo.setTitle(request.getTitle());
        todo.setType(request.getType());
        todo.setRepeatFrequency(request.getRepeatFrequency());
        todo.setRepeatDays(request.getRepeatDays());
        todo.setDueDate(request.getDueDate());
        todo.setDueTime(request.getDueTime());
        todo.setNotificationEnabled(request.getNotificationEnabled());
        todo.setNotificationTime(request.getNotificationTime());

        List<SubTodoUpdateRequest> requestSubTodos = request.getSubTodos();
        List<SubTodo> existingSubTodos = todo.getSubTodos() != null ?
                todo.getSubTodos().stream()
                        .filter(subTodo -> !subTodo.getIsDeleted())
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        if (requestSubTodos == null || requestSubTodos.isEmpty()) {
            existingSubTodos.forEach(subTodo -> subTodo.setIsDeleted(true));
            return TodoListResponse.from(todo);
        }

        Map<Long, SubTodo> existingSubTodoMap = existingSubTodos.stream()
                .collect(Collectors.toMap(SubTodo::getSubTodoId, Function.identity()));

        Set<Long> requestSubTodoIds = requestSubTodos.stream()
                .map(req -> req.getSubTodoId())
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
                        .todoId(todo)
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
            todo.getSubTodos().addAll(newSubTodos);
        }

        return TodoListResponse.from(todo);

    }

    @Override
    @Transactional
    public void deleteTodo(Long bifId, Long todoId) {

        Todo todo = todoRepository.findById(todoId).orElseThrow(() -> new TodoNotFoundException(todoId));

        validateUserPermission(todo, bifId);

        todo.setIsDeleted(true);

    }

    @Override
    @Transactional
    public TodoListResponse completeTodo(Long bifId, Long todoId) {

        Todo todo = todoRepository.findById(todoId).orElseThrow(() -> new TodoNotFoundException(todoId));

        validateUserPermission(todo, bifId);

        if (todo.getType() == TodoTypes.TASK) {
            if (todo.getSubTodos() != null && !todo.getSubTodos().isEmpty()) {
                boolean allCompleted = todo.getSubTodos().stream()
                        .filter(subTodo -> !subTodo.getIsDeleted())
                        .allMatch(SubTodo::getIsCompleted);

                if (!allCompleted) {
                    throw new TodoCompletionException(todoId);
                }
            }

            todo.setIsCompleted(true);
            todo.setCompletedAt(LocalDateTime.now());
        }

        return TodoListResponse.from(todo);

    }

    @Override
    @Transactional
    public TodoListResponse uncompleteTodo(Long bifId, Long todoId) {

        Todo todo = todoRepository.findById(todoId).orElseThrow(() -> new TodoNotFoundException(todoId));

        validateUserPermission(todo, bifId);

        if (todo.getType() == TodoTypes.TASK) {
            todo.setIsCompleted(false);
            todo.setCompletedAt(null);
        } else if (todo.getType() == TodoTypes.ROUTINE) {
            routineCompletionService.uncompleteRoutine(bifId, todoId, LocalDate.now());
        }

        return TodoListResponse.from(todo);

    }

    private void validateUserPermission(Todo todo, Long bifId) {

        if (!todo.getBifId().getId().equals(bifId)) {
            throw new UnauthorizedTodoAccessException(bifId, todo.getTodoId());
        }

    }

}