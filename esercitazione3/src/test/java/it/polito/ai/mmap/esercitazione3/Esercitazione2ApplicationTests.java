package it.polito.ai.mmap.esercitazione3;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polito.ai.mmap.esercitazione3.entity.ChildEntity;
import it.polito.ai.mmap.esercitazione3.entity.RoleEntity;
import it.polito.ai.mmap.esercitazione3.entity.UserEntity;
import it.polito.ai.mmap.esercitazione3.objectDTO.UserDTO;
import it.polito.ai.mmap.esercitazione3.repository.ChildRepository;
import it.polito.ai.mmap.esercitazione3.repository.RoleRepository;
import it.polito.ai.mmap.esercitazione3.repository.UserRepository;
import it.polito.ai.mmap.esercitazione3.resources.PrenotazioneResource;
import it.polito.ai.mmap.esercitazione3.services.JsonHandlerService;
import it.polito.ai.mmap.esercitazione3.services.LineeService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class Esercitazione2ApplicationTests {

    @Value("${superadmin.email}")
    private String superAdminMail;
    @Value("${superadmin.password}")
    private String superAdminPass;

    @Autowired
    JsonHandlerService jsonHandlerService;
    @Autowired
    LineeService lineeService;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ChildRepository childRepository;


    //TODO rimuovere
    @Before
    public void temporaryChildCreation() {
        ChildEntity childEntity;
        childEntity = new ChildEntity("RSSMRA30A01H501I", "Mario", "Rossi");
        childRepository.save(childEntity);

        childEntity = new ChildEntity("SNDPTN80C15H501C", "Sandro", "Pertini");
        childRepository.save(childEntity);

        childEntity = new ChildEntity("CLLCRL80A01H501D", "Carlo", "Collodi");
        childRepository.save(childEntity);
    }


    @Test
    public void getLines() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UserDTO user = new UserDTO();
        user.setEmail(superAdminMail);
        user.setPassword(superAdminPass);
        String json = mapper.writeValueAsString(user);
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        String token = node.get("token").asText();
        logger.info("Test GET /lines ...");
        List<String> expectedResult = new ArrayList<>();
        expectedResult.add("linea1");
        expectedResult.add("linea2");
        String expectedJson = mapper.writeValueAsString(expectedResult);


        this.mockMvc.perform(get("/lines")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));

        logger.info("PASSED");
    }

    @Test
    public void getLine() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UserDTO user = new UserDTO();
        user.setEmail(superAdminMail);
        user.setPassword(superAdminPass);
        String json = mapper.writeValueAsString(user);
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        String token = node.get("token").asText();

        String linea = "linea1";
        logger.info("Test GET /lines/" + linea + " ...");

        this.mockMvc.perform(get("/lines/" + linea)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value(linea));

        logger.info("PASSED");
    }

    @Test
    public void insertReservation_wrongVerso() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UserDTO user = new UserDTO();
        user.setEmail(superAdminMail);
        user.setPassword(superAdminPass);
        String json = mapper.writeValueAsString(user);
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        String token = node.get("token").asText();

        PrenotazioneResource res = PrenotazioneResource.builder().cfChild("RSSMRA30A01H501I").idFermata(1).verso(false).build();
        String resJson = mapper.writeValueAsString(res);

        logger.info("Inserimento errato " + res + "...");
        logger.info("POST /reservations/linea1/2019-01-01/ con verso errato ...");

        this.mockMvc.perform(post("/reservations/linea1/2019-01-01/")
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());

        logger.info("PASSED");

    }

    @Test
    public void insertReservation_correctVerso() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UserDTO user = new UserDTO();
        user.setEmail(superAdminMail);
        user.setPassword(superAdminPass);
        String json = mapper.writeValueAsString(user);
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        String token = node.get("token").asText();

        PrenotazioneResource res = PrenotazioneResource.builder().cfChild("RSSMRA30A01H501I").idFermata(1).verso(true).build();
        String resJson = mapper.writeValueAsString(res);

        logger.info("Inserimento corretto " + res + "...");
        logger.info("POST /reservations/linea1/2019-01-01/ con verso corretto ...");

        MvcResult result1 = this.mockMvc.perform(post("/reservations/linea1/2019-01-01/")
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        String idRes = result1.getResponse().getContentAsString();
        logger.info("PASSED");

        logger.info("Ripristino stato precedente...");
        this.mockMvc.perform(delete("/reservations/linea1/2019-01-01/" + idRes)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        logger.info("DONE");

    }

    @Test
    public void insertReservation_wrongLine() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UserDTO user = new UserDTO();
        user.setEmail(superAdminMail);
        user.setPassword(superAdminPass);
        String json = mapper.writeValueAsString(user);
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        String token = node.get("token").asText();

        PrenotazioneResource res = PrenotazioneResource.builder().cfChild("RSSMRA30A01H501I").idFermata(1).verso(false).build();
        String resJson = mapper.writeValueAsString(res);

        logger.info("Inserimento errato " + res + "...");
        logger.info("POST /reservations/linea3/2019-01-01/ con linea errata ...");

        this.mockMvc.perform(post("/reservations/linea1/2019-01-01/")
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());

        logger.info("PASSED");

    }

    @Test
    public void getReservation_check() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UserDTO user = new UserDTO();
        user.setEmail(superAdminMail);
        user.setPassword(superAdminPass);
        String json = mapper.writeValueAsString(user);
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        String token = node.get("token").asText();

        PrenotazioneResource res = PrenotazioneResource.builder().cfChild("SNDPTN80C15H501C").idFermata(1).verso(true).build();
        String resJson = mapper.writeValueAsString(res);

        logger.info("Inserimento " + res + "...");
        MvcResult result1 = this.mockMvc.perform(post("/reservations/linea1/2019-01-01/")
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String idRes = result1.getResponse().getContentAsString();
        logger.info("Inserito correttamente!");

        logger.info("Controllo reservation " + idRes + " ...");
        this.mockMvc.perform(get("/reservations/linea1/2019-01-01/" + idRes)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cfChild").value(res.getCfChild()))
                .andExpect(jsonPath("$.idFermata").value(res.getIdFermata()))
                .andExpect(jsonPath("$.verso").value(res.getVerso()));
        logger.info("PASSED");

        logger.info("Ripristino stato precedente...");
        this.mockMvc.perform(delete("/reservations/linea1/2019-01-01/" + idRes)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        logger.info("DONE");

    }

    @Test
    public void insertReservation_duplicate() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UserDTO user = new UserDTO();
        user.setEmail(superAdminMail);
        user.setPassword(superAdminPass);
        String json = mapper.writeValueAsString(user);
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        String token = node.get("token").asText();

        PrenotazioneResource res = PrenotazioneResource.builder().cfChild("SNDPTN80C15H501C").idFermata(1).verso(true).build();
        String resJson = mapper.writeValueAsString(res);
        logger.info("Inserimento " + res + "...");

        MvcResult result1 = this.mockMvc.perform(post("/reservations/linea1/2019-01-01/")
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();

        String idRes = result1.getResponse().getContentAsString();
        logger.info("Inserito correttamente!");

        logger.info("POST /reservations/linea1/2019-01-01/ duplicato ...");
        this.mockMvc.perform(post("/reservations/linea1/2019-01-01/")
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());
        logger.info("PASSED");

        logger.info("Ripristino stato precedente...");
        this.mockMvc.perform(delete("/reservations/linea1/2019-01-01/" + idRes)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        logger.info("DONE");
    }

    @Test
    public void getReservation_checkReservationPositionInLine() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UserDTO user = new UserDTO();
        user.setEmail(superAdminMail);
        user.setPassword(superAdminPass);
        String json = mapper.writeValueAsString(user);
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        String token = node.get("token").asText();

        PrenotazioneResource res = PrenotazioneResource.builder().cfChild("SNDPTN80C15H501C").idFermata(1).verso(true).build();
        String resJson = mapper.writeValueAsString(res);
        logger.info("Inserimento " + res + "...");

        MvcResult result1 = this.mockMvc.perform(post("/reservations/linea1/2019-01-01/")
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();

        String idRes = result1.getResponse().getContentAsString();
        logger.info("Inserito correttamente!");

        logger.info("Controllo posizione nomeAlunno nelle linee di " + idRes);
        logger.info("GET /reservations/linea1/2019-01-01/ per controllo presenza utente ...");
        this.mockMvc.perform(get("/reservations/linea1/2019-01-01/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alunniPerFermataAndata[0].alunni[0].codiceFiscale").value(res.getCfChild()));
        logger.info("PASSED");

        logger.info("Ripristino stato precedente...");
        this.mockMvc.perform(delete("/reservations/linea1/2019-01-01/" + idRes)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        logger.info("DONE");
    }

    @Test
    public void putReservation_updateWrong() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UserDTO user = new UserDTO();
        user.setEmail(superAdminMail);
        user.setPassword(superAdminPass);
        String json = mapper.writeValueAsString(user);
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        String token = node.get("token").asText();

        PrenotazioneResource res = PrenotazioneResource.builder().cfChild("SNDPTN80C15H501C").idFermata(1).verso(true).build();
        String resJson = mapper.writeValueAsString(res);
        logger.info("Inserimento " + res + "...");

        MvcResult result1 = this.mockMvc.perform(post("/reservations/linea1/2019-01-01/")
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();

        String idRes = result1.getResponse().getContentAsString();
        logger.info("Inserito correttamente!");


        logger.info("Modifico Prenotazione in modo errato");
        PrenotazioneResource resWrong = PrenotazioneResource.builder().cfChild("CLLCRL80A01H501D").idFermata(5).verso(true).build();
        String resWrongJson = mapper.writeValueAsString(resWrong);

        this.mockMvc.perform(put("/reservations/linea1/2019-01-01/" + idRes)
                .contentType(MediaType.APPLICATION_JSON).content(resWrongJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());
        logger.info("PASSED");

        logger.info("Ripristino stato precedente...");
        this.mockMvc.perform(delete("/reservations/linea1/2019-01-01/" + idRes)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        logger.info("DONE");
    }

    @Test
    public void putReservation_updateCorrect_checkPosition() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UserDTO user = new UserDTO();
        user.setEmail(superAdminMail);
        user.setPassword(superAdminPass);
        String json = mapper.writeValueAsString(user);
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        String token = node.get("token").asText();

        PrenotazioneResource res = PrenotazioneResource.builder().cfChild("SNDPTN80C15H501C").idFermata(1).verso(true).build();
        String resJson = mapper.writeValueAsString(res);
        logger.info("Inserimento e controllo posizione " + res + "...");
        MvcResult result1 = this.mockMvc.perform(post("/reservations/linea1/2019-01-01/")
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();

        String idRes = result1.getResponse().getContentAsString();

        this.mockMvc.perform(get("/reservations/linea1/2019-01-01/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alunniPerFermataAndata[0].alunni[0].codiceFiscale").value(res.getCfChild()));
        logger.info("Inserito e controllato correttamente!");


        logger.info("Modifico Prenotazione ...");
        PrenotazioneResource resCorrect = PrenotazioneResource.builder().cfChild("CLLCRL80A01H501D").idFermata(5).verso(false).build();
        String resCorrectJson = mapper.writeValueAsString(resCorrect);

        this.mockMvc.perform(put("/reservations/linea1/2019-01-01/" + idRes)
                .contentType(MediaType.APPLICATION_JSON).content(resCorrectJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        logger.info("Controllo nuova posizione prenotazione");
        this.mockMvc.perform(get("/reservations/linea1/2019-01-01/")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alunniPerFermataRitorno[0].alunni[0].codiceFiscale").value(resCorrect.getCfChild()));

        logger.info("PASSED");
        logger.info("Ripristino stato precedente...");
        this.mockMvc.perform(delete("/reservations/linea1/2019-01-01/" + idRes)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        logger.info("DONE");
    }

    @Test
    public void deleteReservation_randomID() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        UserDTO user = new UserDTO();
        user.setEmail(superAdminMail);
        user.setPassword(superAdminPass);
        String json = mapper.writeValueAsString(user);
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        String token = node.get("token").asText();

        logger.info("Cancellazione a caso errata con numero...");
        this.mockMvc.perform(delete("/reservations/linea1/2019-01-01/12345")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        logger.info("Cancellazione a caso errata con objectID...");
        this.mockMvc.perform(delete("/reservations/linea1/2019-01-01/5cc9c667c947dc1d2eb496ee")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());
        logger.info("DONE");
    }


}
