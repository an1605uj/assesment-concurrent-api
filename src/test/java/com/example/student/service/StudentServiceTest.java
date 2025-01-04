package com.example.student.service;

import com.example.student.Repository.StudentRepository;
import com.example.student.dtos.StudentDto;
import com.example.student.entity.Student;
import com.example.student.utils.Constant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private Logger log;

    @InjectMocks
    private StudentService studentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        studentService = new StudentService(studentRepository,redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testInit() {
        studentService.init();
        verify(log, times(1)).info("StudentService initialized with thread pool size: {}", Constant.THREAD_POOL_SIZE);
    }

    @Test
    void testCleanup() {
        studentService.cleanup();
        verify(log, times(1)).info("Shutting down executor service.");
    }

    @Test
    void testGetAllStudents() {
        // Prepare mock data
        Student student1 = new Student();
        student1.setId(1L);
        student1.setName("John Doe");

        Student student2 = new Student();
        student2.setId(2L);
        student2.setName("Jane Doe");

        List<Student> mockStudents = Arrays.asList(student1, student2);
        when(studentRepository.findAll()).thenReturn(mockStudents);
        List<Student> result = studentService.getAllStudents();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John Doe", result.get(0).getName());
        assertEquals("Jane Doe", result.get(1).getName());
        verify(studentRepository, times(1)).findAll();
    }
    @Test
    void testGetStudentByIdAsync_cacheHit() {
        Long studentId = 1L;
        Student student = new Student();
        student.setId(studentId);
        student.setName("John Doe");

        when(redisTemplate.opsForValue().get("student_1")).thenReturn(student);

        CompletableFuture<Student> result = studentService.getStudentByIdAsync(studentId);

        assertNotNull(result);
    }

    @Test
    void testGetStudentByIdAsync_cacheMiss() {
        Long studentId = 1L;

        // Simulate cache miss and database fetch
        when(redisTemplate.opsForValue().get("student_1")).thenReturn(null);
        Student student = new Student();
        student.setId(studentId);
        student.setName("John Doe");
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        CompletableFuture<Student> result = studentService.getStudentByIdAsync(studentId);

        assertNotNull(result);
        assertEquals(student, result.join());
    }

    @Test
    void testSaveStudentAsync() {
        StudentDto studentDto = new StudentDto();
        studentDto.setName("Jane Doe");
        studentDto.setAge(20);

        Student savedStudent = new Student();
        savedStudent.setId(1L);
        savedStudent.setName("Jane Doe");
        savedStudent.setAge(20);

        when(studentRepository.save(any(Student.class))).thenReturn(savedStudent);

        CompletableFuture<Student> result = studentService.saveStudentAsync(studentDto);

        assertNotNull(result);
        assertEquals(savedStudent, result.join());
    }

    @Test
    void testUpdateStudentAsync_studentExist() {
        Long studentId = 1L;
        Student studentToUpdate = new Student();
        studentToUpdate.setName("Updated Name");

        Student updatedStudent = new Student();
        updatedStudent.setId(studentId);
        updatedStudent.setName("Updated Name");

        when(studentRepository.existsById(studentId)).thenReturn(true);
        when(studentRepository.save(any(Student.class))).thenReturn(updatedStudent);

        CompletableFuture<Student> result = studentService.updateStudentAsync(studentId, studentToUpdate);

        assertNotNull(result);
        assertEquals(updatedStudent, result.join());
    }

    @Test
    void testUpdateStudentAsync_studentDoesNotExist() {
        Long studentId = 1L;
        Student student = new Student();
        student.setId(studentId);
        student.setName("Non-Existent Student");
        student.setAge(22);
        when(studentRepository.existsById(studentId)).thenReturn(false);

        CompletableFuture<Student> result = studentService.updateStudentAsync(studentId, student);

        assertNotNull(result);
        assertNull(result.join());
        verify(studentRepository, times(1)).existsById(studentId);
        verify(studentRepository, times(0)).save(any(Student.class));
    }

    @Test
    void testDeleteStudentAsync_studentExists() {
        Long studentId = 1L;

        when(studentRepository.existsById(studentId)).thenReturn(true);

        CompletableFuture<Boolean> result = studentService.deleteStudentAsync(studentId);

        assertNotNull(result);
        assertTrue(result.join());
        verify(studentRepository, times(1)).deleteById(studentId);
    }
    @Test
    void testDeleteStudentAsync_studentDoesNotExist() {
        Long studentId = 1L;
        when(studentRepository.existsById(studentId)).thenReturn(false);
        CompletableFuture<Boolean> result = studentService.deleteStudentAsync(studentId);
        assertNotNull(result);
        assertFalse(result.join());
        verify(studentRepository, times(1)).existsById(studentId);
        verify(studentRepository, times(0)).deleteById(studentId);
    }
}
