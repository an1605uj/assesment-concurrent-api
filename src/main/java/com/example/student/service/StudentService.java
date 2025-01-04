package com.example.student.service;

import com.example.student.Repository.StudentRepository;
import com.example.student.utils.Constant;
import com.example.student.dtos.StudentDto;
import com.example.student.entity.Student;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class StudentService {
    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(Constant.THREAD_POOL_SIZE);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository, RedisTemplate<String, Object> redisTemplate) {
        this.studentRepository = studentRepository;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        log.info("StudentService initialized with thread pool size: {}", Constant.THREAD_POOL_SIZE);
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down executor service.");
        executorService.shutdown();
    }

    public List<Student> getAllStudents() {
        log.info("Fetching all students.");
        return studentRepository.findAll();
    }

    public CompletableFuture<Student> getStudentByIdAsync(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            String cacheKey = Constant.STUDENT_KEY_PREFIX + id;
            Student cachedStudent = (Student) redisTemplate.opsForValue().get(cacheKey);

            if (cachedStudent != null) {
                log.info("Cache hit for student with id: {}", id);
                return cachedStudent;
            }

            log.info("Cache miss for student with id: {}", id);
            Optional<Student> student = studentRepository.findById(id);
            student.ifPresent(value -> redisTemplate.opsForValue().set(cacheKey, value, Constant.REDIS_EXPIRY_TIME, TimeUnit.MINUTES));

            return student.orElse(null);
        }, executorService);
    }

    public CompletableFuture<Student> saveStudentAsync(StudentDto studentDto) {
        return CompletableFuture.supplyAsync(() -> {
            Student student = new Student();
            student.setAge(studentDto.getAge());
            student.setName(studentDto.getName());

            Student savedStudent = studentRepository.save(student);
            String cacheKey = Constant.STUDENT_KEY_PREFIX + savedStudent.getId();
            redisTemplate.opsForValue().set(cacheKey, savedStudent, Constant.REDIS_EXPIRY_TIME, TimeUnit.MINUTES);

            log.info("Saved student with id: {} and cached it.", savedStudent.getId());
            return savedStudent;
        }, executorService);
    }

    public CompletableFuture<Student> updateStudentAsync(Long id, Student student) {
        return CompletableFuture.supplyAsync(() -> {
            if (!studentRepository.existsById(id)) {
                log.warn("Student with id: {} not found for update.", id);
                return null;
            }

            student.setId(id);
            Student updatedStudent = studentRepository.save(student);

            String cacheKey = Constant.STUDENT_KEY_PREFIX + id;
            redisTemplate.opsForValue().set(cacheKey, updatedStudent, Constant.REDIS_EXPIRY_TIME, TimeUnit.MINUTES);

            log.info("Updated student with id: {} and refreshed cache.", updatedStudent.getId());
            return updatedStudent;
        }, executorService);
    }

    public CompletableFuture<Boolean> deleteStudentAsync(Long id) {
        return CompletableFuture.supplyAsync(() -> {
            if (!studentRepository.existsById(id)) {
                log.warn("Student with id: {} not found for deletion.", id);
                return false;
            }

            studentRepository.deleteById(id);

            String cacheKey = Constant.STUDENT_KEY_PREFIX + id;
            redisTemplate.delete(cacheKey);

            log.info("Deleted student with id: {} and cleared cache.", id);
            return true;
        }, executorService);
    }
}
