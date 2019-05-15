package it.polito.ai.mmap.esercitazione3;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.polito.ai.mmap.esercitazione3.entity.ActivationTokenEntity;
import it.polito.ai.mmap.esercitazione3.entity.RecoverTokenEntity;
import it.polito.ai.mmap.esercitazione3.entity.UserEntity;
import it.polito.ai.mmap.esercitazione3.objectDTO.UserDTO;
import it.polito.ai.mmap.esercitazione3.repository.ActivationTokenRepository;
import it.polito.ai.mmap.esercitazione3.repository.RecoverTokenRepository;
import it.polito.ai.mmap.esercitazione3.repository.UserRepository;
import it.polito.ai.mmap.esercitazione3.services.JsonHandlerService;
import it.polito.ai.mmap.esercitazione3.services.MongoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class Esercitazione3ApplicationTests {

    @Autowired
    JsonHandlerService jsonHandlerService;
    @Autowired
    MongoService mongoService;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RecoverTokenRepository recoverTokenRepository;
    @Autowired
    private ActivationTokenRepository activationTokenRepository;


    @Test
    public void postLogin_correct() throws Exception {
        logger.info("Test POST /login ...");
        UserDTO user = new UserDTO();
        user.setEmail("applicazioni.internet.mmap@gmail.com");
        user.setPassword("mmapmmap1!");
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(user);

        this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());

        logger.info("PASSED");
    }

    @Test
    public void postLogin_incorrect() throws Exception {
        logger.info("Test POST /login ...");
        UserDTO user = new UserDTO();
        user.setEmail("applicazioni.internet.mmap@gmail.com");
        user.setPassword("mmapmmap1");
        ObjectMapper mapper = new ObjectMapper();
        String json1 = mapper.writeValueAsString(user);

        this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json1))
                .andExpect(status().isUnauthorized());

        user.setEmail("applicazioni.internet.mmapgmail.com");
        user.setPassword("mmapmmap1");
        String json2 = mapper.writeValueAsString(user);

        this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json2))
                .andExpect(status().isUnauthorized());

        user.setEmail("applicazioni.internet.mmap@gmail.com");
        user.setPassword("1");
        String json3 = mapper.writeValueAsString(user);

        this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json3))
                .andExpect(status().isUnauthorized());

        logger.info("PASSED");
    }

    @Test
    public void postRegister_correct() throws Exception {
        logger.info("Test POST /register ...");
        UserDTO user = new UserDTO();
        user.setEmail("pieromacaluso8@gmail.com");
        user.setPassword("123456789");
        user.setPassMatch("123456789");
        ObjectMapper mapper = new ObjectMapper();
        String json1 = mapper.writeValueAsString(user);

        this.mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(json1))
                .andExpect(status().isOk());

        logger.info("PASSED");
        if (this.userRepository.findByUsername("pieromacaluso8@gmail.com").isPresent()) {
            this.userRepository.delete(this.userRepository.findByUsername("pieromacaluso8@gmail.com").get());
        }
    }

    @Test
    public void postRegister_duplicate() throws Exception {
        logger.info("Test POST /register duplicate ...");
        UserDTO user = new UserDTO();
        user.setEmail("pieromacaluso8@gmail.com");
        user.setPassword("123456789");
        user.setPassMatch("123456789");
        ObjectMapper mapper = new ObjectMapper();
        String json1 = mapper.writeValueAsString(user);

        this.mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(json1))
                .andExpect(status().isOk());
        this.mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(json1))
                .andExpect(status().isInternalServerError());

        logger.info("PASSED");
        if (this.userRepository.findByUsername("pieromacaluso8@gmail.com").isPresent()) {
            this.userRepository.delete(this.userRepository.findByUsername("pieromacaluso8@gmail.com").get());
        }
    }

    @Test
    public void postRegister_incorrect() throws Exception {
        logger.info("Test POST /register incorrect ...");
        UserDTO user = new UserDTO();
        logger.info("Passwords does not match ...");

        user.setEmail("pieromacaluso8@gmail.com");
        user.setPassword("123456789");
        user.setPassMatch("12345678");
        ObjectMapper mapper = new ObjectMapper();
        String json1 = mapper.writeValueAsString(user);

        this.mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(json1))
                .andExpect(status().isInternalServerError());

        logger.info("Password not valid ...");

        user.setEmail("pieromacaluso8@gmail.com");
        user.setPassword("1");
        user.setPassMatch("1");
        String json2 = mapper.writeValueAsString(user);

        this.mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(json2))
                .andExpect(status().isInternalServerError());

        logger.info("Email not valid ...");
        user.setEmail("pieromacaluso8gmail.com");
        user.setPassword("123456789");
        user.setPassMatch("123456789");
        String json3 = mapper.writeValueAsString(user);

        this.mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(json3))
                .andExpect(status().isInternalServerError());

        logger.info("PASSED");
        if (this.userRepository.findByUsername("pieromacaluso8@gmail.com").isPresent()) {
            this.userRepository.delete(this.userRepository.findByUsername("pieromacaluso8@gmail.com").get());
        }
    }

    @Test
    public void getConfirmRandomUUID_correct() throws Exception {
        logger.info("Test GET /confirm/{randomUUID} correct ...");
        UserDTO user = new UserDTO();
        user.setEmail("pieromacaluso8@gmail.com");
        user.setPassword("123456789");
        user.setPassMatch("123456789");
        ObjectMapper mapper = new ObjectMapper();
        String json1 = mapper.writeValueAsString(user);

        this.mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(json1))
                .andExpect(status().isOk());

        Optional<UserEntity> checkUser = this.userRepository.findByUsername("pieromacaluso8@gmail.com");
        assert checkUser.isPresent();
        Optional<ActivationTokenEntity> activationCheck = this.activationTokenRepository.findByUserId(checkUser.get().getId());
        assert activationCheck.isPresent();
        String UUID = activationCheck.get().getId().toString();
        this.mockMvc.perform(get("/confirm/" + UUID))
                .andExpect(status().isOk());

        logger.info("PASSED");
        this.userRepository.delete(checkUser.get());

    }

    @Test
    public void getConfirmRandomUUID_incorrect() throws Exception {
        logger.info("Test GET /confirm/{randomUUID} incorrect ...");

        this.mockMvc.perform(get("/confirm/123456789"))
                .andExpect(status().isNotFound());

        logger.info("PASSED");
    }

    @Test
    public void postRecover_always200() throws Exception {
        logger.info("Test ALL /recover");
        String email = "pieromacaluso8@gmail.com";
        UserDTO user = new UserDTO();
        user.setEmail(email);
        user.setPassword("123456789");
        user.setPassMatch("123456789");
        ObjectMapper mapper = new ObjectMapper();
        String json1 = mapper.writeValueAsString(user);

        this.mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(json1))
                .andExpect(status().isOk());

        Optional<UserEntity> check = this.userRepository.findByUsername("pieromacaluso8@gmail.com");
        assert check.isPresent();
        Optional<ActivationTokenEntity> activationCheck = this.activationTokenRepository.findByUserId(check.get().getId());
        assert activationCheck.isPresent();
        String UUID = activationCheck.get().getId().toString();
        this.mockMvc.perform(get("/confirm/" + UUID))
                .andExpect(status().isOk());

        this.mockMvc.perform(post("/recover").contentType(MediaType.APPLICATION_JSON).content(email))
                .andExpect(status().isOk());

        Optional<RecoverTokenEntity> recoverCheck = this.recoverTokenRepository.findByUserId(check.get().getId());
        assert recoverCheck.isPresent();
        // TODO: da modificare in base a cambiamenti Confirmation RandomUUID
        UUID = recoverCheck.get().getId().toString();
        this.mockMvc.perform(get("/recover/" + UUID))
                .andExpect(status().isOk());
        UserDTO user1 = new UserDTO();
        user1.setPassword("987654321");
        user1.setPassMatch("987654321");
        String json2 = mapper.writeValueAsString(user1);
        this.mockMvc.perform(post("/recover/" + UUID).contentType(MediaType.APPLICATION_JSON).content(json2))
                .andExpect(status().isOk());

        user.setPassword("987654321");
        String json3 = mapper.writeValueAsString(user);

        this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json3))
                .andExpect(status().isOk());

        email = "ciao";
        this.mockMvc.perform(post("/recover").contentType(MediaType.APPLICATION_JSON).content(email))
                .andExpect(status().isOk());

        logger.info("PASSED");
        Optional<UserEntity> userCheck = this.userRepository.findByUsername("pieromacaluso8@gmail.com");
        if (userCheck.isPresent()) {
            Optional<RecoverTokenEntity> tokenCheck = this.recoverTokenRepository.findById(userCheck.get().getId());
            tokenCheck.ifPresent(recoverTokenEntity -> this.recoverTokenRepository.delete(recoverTokenEntity));
            this.userRepository.delete(userCheck.get());
        }
    }

}