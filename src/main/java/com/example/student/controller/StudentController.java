package com.example.student.controller;

import com.example.student.dtos.StudentDto;
import com.example.student.entity.Student;
import com.example.student.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping("/all-students")
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/{id}/student")
    public CompletableFuture<ResponseEntity<?>> getStudent(@PathVariable Long id) {
        return studentService.getStudentByIdAsync(id)
                .thenApply(student -> {
                    if (student == null) {
                        Map<String, String> response = new HashMap<>();
                        response.put("message", "Student with ID : " + id + " not found");
                        return ResponseEntity.status(404).body(response);
                    }
                    return ResponseEntity.ok(student);
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Error retrieving student: " + ex.getMessage());
                    return ResponseEntity.internalServerError().body(response);
                });
    }

    @PostMapping("/add-student")
    public CompletableFuture<ResponseEntity<?>> createStudent(@RequestBody StudentDto studentDto) {
        return studentService.saveStudentAsync(studentDto)
                .thenApply(savedStudent -> {
                    if (savedStudent == null) {
                        return ResponseEntity.status(500).body("Error creating student: Student could not be saved");
                    }
                    return ResponseEntity.ok(savedStudent);
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.internalServerError()
                            .body("Error creating student: " + ex.getMessage());
                });
    }

    @PutMapping("/{id}/update-student")
    public CompletableFuture<ResponseEntity<?>> updateStudent(@PathVariable Long id, @RequestBody Student student) {
        return studentService.updateStudentAsync(id, student)
                .thenApply(updatedStudent -> {
                    if (updatedStudent == null) {
                        Map<String, String> response = new HashMap<>();
                        response.put("message", "Student with ID " + id + " not found");
                        return ResponseEntity.status(404).body(response);  // Wrap the message in a map
                    }
                    return ResponseEntity.ok(updatedStudent);
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "Error updating student: " + ex.getMessage());
                    return ResponseEntity.internalServerError().body(response);
                });
    }


    @DeleteMapping("/{id}/delete-student")
    public CompletableFuture<ResponseEntity<?>> deleteStudent(@PathVariable Long id) {
        return studentService.deleteStudentAsync(id)
                .thenApply(deleted -> {
                    if (deleted) {
                        return ResponseEntity.noContent().build();
                    }
                    return ResponseEntity.status(404).body("Student with ID " + id + " not found");
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return ResponseEntity.internalServerError()
                            .body("Error deleting student: " + ex.getMessage());
                });
    }
}
