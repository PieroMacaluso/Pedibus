package it.polito.ai.mmap.pedibus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polito.ai.mmap.pedibus.entity.*;
import it.polito.ai.mmap.pedibus.objectDTO.ChildDTO;
import it.polito.ai.mmap.pedibus.objectDTO.UserDTO;
import it.polito.ai.mmap.pedibus.repository.*;
import it.polito.ai.mmap.pedibus.resources.GetReservationsNomeLineaDataVersoResource;
import it.polito.ai.mmap.pedibus.resources.ReservationResource;
import it.polito.ai.mmap.pedibus.services.*;
import org.bson.types.ObjectId;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * - Usando i bimbi e gli utenti di test qualsiasi aggiunta o modifica viene cancellata dopo ogni test
 * - Non supporre che tra 4, n giorni le uniche prenotazioni siano quelle introdotte come test
 * - usare solo gli endpoint http che si stanno testando (a meno che lo si faccia per comodità), per il resto fare accesso diretto al db
 * - 2 funzioni di comodità:
 *         - loginAsSystemAdmin
 *         - inserimentoReservationGenitore
 */


@RunWith(SpringRunner.class)

@SpringBootTest
@AutoConfigureMockMvc
public class Esercitazione2ApplicationTests {

    @Value("${superadmin.email}")
    private String superAdminMail;
    @Value("${superadmin.password}")
    private String superAdminPass;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JsonHandlerService jsonHandlerService;
    @Autowired
    LineeService lineeService;
    @Autowired
    UserService userService;

    private Logger logger = LoggerFactory.getLogger(Esercitazione2ApplicationTests.class);
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ChildRepository childRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    LineaRepository lineaRepository;

    @Autowired
    FermataRepository fermataRepository;

    @Autowired
    ReservationRepository reservationRepository;

    @Autowired
    PasswordEncoder passwordEncoder;


    Map<Integer, ChildEntity> childMap = new HashMap<>();
    Map<String, UserDTO> userDTOMap = new HashMap<>();
    Map<String, UserEntity> userEntityMap = new HashMap<>();
    RoleEntity roleUser;
    RoleEntity roleAdmin;


    @PostConstruct
    public void postInit() {
        roleUser = roleRepository.findByRole("ROLE_USER");
        roleAdmin = roleRepository.findByRole("ROLE_ADMIN");
        childMap.put(0, new ChildEntity("RSSMRA30A01H501I", "Mario", "Rossi"));
        childMap.put(1, new ChildEntity("SNDPTN80C15H501C", "Sandro", "Pertini"));
        childMap.put(2, new ChildEntity("CLLCRL80A01H501D", "Carlo", "Collodi"));

        userDTOMap.put("testGenitore", new UserDTO("testGenitore@test.it", "321@%$User", "321@%$User"));
        userDTOMap.put("testNonGenitore", new UserDTO("testNonGenitore@test.it", "321@%$User", "321@%$User"));
        userDTOMap.put("testNonno", new UserDTO("testNonno@test.it", "321@%$User", "321@%$User"));

        userEntityMap.put("testGenitore", new UserEntity(userDTOMap.get("testGenitore"), new HashSet<>(Arrays.asList(roleUser)), passwordEncoder, new HashSet<>(Arrays.asList(childMap.get(0).getCodiceFiscale()))));
        userEntityMap.put("testNonGenitore", new UserEntity(userDTOMap.get("testNonGenitore"), new HashSet<>(Arrays.asList(roleUser)), passwordEncoder));
        userEntityMap.put("testNonno", new UserEntity(userDTOMap.get("testNonno"), new HashSet<>(Arrays.asList(roleAdmin)), passwordEncoder));

    }

    @Before
    public void setUpMethod() {
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);
        logger.info("Il nonno sarà admin della linea: " + lineaEntity.getId());

        childRepository.saveAll(childMap.values());
        userEntityMap.values().forEach(userEntity -> {
            userEntity.setEnabled(true);
            if (userEntity.getRoleList().contains(roleAdmin))
                lineeService.addAdminLine(userEntity.getUsername(), lineaEntity.getId());
            userRepository.save(userEntity);
        });
    }

    @After
    public void tearDownMethod() {
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);

        childMap.values().forEach(childEntity ->
        {
            reservationRepository.deleteAllByCfChild(childEntity.getCodiceFiscale());
            childRepository.delete(childEntity);
        });
        userEntityMap.values().forEach(userEntity -> {
            if (userEntity.getRoleList().contains(roleAdmin))
                lineeService.delAdminLine(userEntity.getUsername(), lineaEntity.getId());
            userRepository.delete(userEntity);
        });
    }

    /**
     * Controlla che GET /lines funzioni come da specifiche
     *
     * @throws Exception
     */
    @Test
    public void getLines() throws Exception {
        String token = loginAsSystemAdmin();

        logger.info("Test GET /lines ...");
        List<String> expectedResult = lineaRepository.findAll().stream().map(LineaEntity::getId).collect(Collectors.toList());
        String expectedJson = objectMapper.writeValueAsString(expectedResult);

        this.mockMvc.perform(get("/lines")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));

        logger.info("PASSED");
    }

    /**
     * Controlla che GET /lines/{nome_linea} funzioni come da specifiche
     *
     * @throws Exception
     */
    @Test
    public void getLine() throws Exception {
        String token = loginAsSystemAdmin();

        String lineaID = lineaRepository.findAll().get(0).getId();
        logger.info("Test GET /lines/" + lineaID + " ...");

        this.mockMvc.perform(get("/lines/" + lineaID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lineaID));

        logger.info("PASSED");
    }

    /**
     * Controlla POST /reservations/{nome_linea}/{data} con dei dati corretti
     *
     * @throws Exception
     */
    @Test
    public void postReservation_Correct() throws Exception {
        String token = loginAsSystemAdmin();
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);
        ReservationResource res = ReservationResource.builder().cfChild(childMap.get(0).getCodiceFiscale()).idFermata(lineaEntity.getAndata().get(0)).verso(true).build();
        String resJson = objectMapper.writeValueAsString(res);

        logger.info("Inserimento " + res + "...");
        logger.info("POST /reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS));

        this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS))
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        logger.info("PASSED");
    }

    /**
     * Controlla POST /reservations/{nome_linea}/{data} con un inserimento duplicato
     *
     * @throws Exception
     */
    @Test
    public void postReservation_Duplicate() throws Exception {
        String token = loginAsSystemAdmin();
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);
        ReservationResource res = ReservationResource.builder().cfChild(childMap.get(1).getCodiceFiscale()).idFermata(lineaEntity.getAndata().get(0)).verso(true).build();
        String resJson = objectMapper.writeValueAsString(res);

        logger.info("Inserimento " + res + "...");
        logger.info("POST /reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS));

        this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS))
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        logger.info("Inserita correttamente!");
        logger.info("Inserimento duplicato");
        logger.info("POST /reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS) + "/ duplicato ...");

        this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS))
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());

        logger.info("PASSED");
    }


    /**
     * Controlla che tramite POST /reservations/{nome_linea}/{data}
     * non si possa inserire una prenotazione per una fermata nel verso sbagliato
     *
     * @throws Exception
     */
    @Test
    public void postReservation_WrongVerso() throws Exception {
        String token = loginAsSystemAdmin();
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);
        ReservationResource res = ReservationResource.builder().cfChild(childMap.get(0).getCodiceFiscale()).idFermata(lineaEntity.getAndata().get(0)).verso(false).build();
        String resJson = objectMapper.writeValueAsString(res);

        logger.info("Inserimento con verso errato di " + res + "...");
        logger.info("POST /reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS));

        this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS))
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());

        logger.info("PASSED");
    }


    /**
     * Controlla che tramite POST /reservations/{nome_linea}/{data}
     * non si possa inserire una prenotazione per una linea indicando la fermata di un'altra linea
     *
     * @throws Exception
     */
    @Test
    public void postReservation_WrongLine() throws Exception {
        String token = loginAsSystemAdmin();
        List<LineaEntity> lineaEntityList = lineaRepository.findAll();
        ReservationResource res = ReservationResource.builder().cfChild(childMap.get(0).getCodiceFiscale()).idFermata(lineaEntityList.get(0).getAndata().get(0)).verso(true).build();
        String resJson = objectMapper.writeValueAsString(res);

        logger.info("Inserimento errato " + res + "...");
        logger.info("POST /reservations/" + lineaEntityList.get(1).getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS));

        this.mockMvc.perform(post("/reservations/" + lineaEntityList.get(1).getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS))
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());

        logger.info("PASSED");
    }

    /**
     * Controlla GET /reservations/{nome_linea}/{data}/{reservation_id} con dati corretti
     *
     * @throws Exception
     */
    @Test
    public void getReservation() throws Exception {
        String token = loginAsSystemAdmin();
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);
        ReservationResource res = ReservationResource.builder().cfChild(childMap.get(1).getCodiceFiscale()).idFermata(lineaEntity.getRitorno().get(0)).verso(false).build();
        String resJson = objectMapper.writeValueAsString(res);

        logger.info("Inserimento " + res + "...");
        MvcResult result1 = this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS))
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String idRes = objectMapper.readValue(result1.getResponse().getContentAsString(), String.class);
        logger.info("Prenotazione inserita correttamente!");

        logger.info("Controllo prenotazione " + idRes + " ...");
        this.mockMvc.perform(get("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS) + "/" + idRes)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cfChild").value(res.getCfChild()))
                .andExpect(jsonPath("$.idFermata").value(res.getIdFermata()))
                .andExpect(jsonPath("$.verso").value(res.getVerso()));

        logger.info("PASSED");
    }

    /**
     * Controlla GET /reservations/verso/{nome_linea}/{data}/{verso}
     * Tale endPoint deve ritornare tutte le reservations per una determinata combinazione di linea, data e verso.
     *
     * @throws Exception
     */
    @Test
    public void getReservationsTowards() throws Exception {
        String token = loginAsSystemAdmin();

        LineaEntity lineaEntity = lineaRepository.findAll().get(0);
        ReservationResource resTrue = ReservationResource.builder().cfChild(childMap.get(1).getCodiceFiscale()).idFermata(lineaEntity.getAndata().get(0)).verso(true).build();
        ReservationResource resFalse = ReservationResource.builder().cfChild(childMap.get(1).getCodiceFiscale()).idFermata(lineaEntity.getRitorno().get(0)).verso(false).build();
        String resTrueJson = objectMapper.writeValueAsString(resTrue);
        String resFalseJson = objectMapper.writeValueAsString(resFalse);

        logger.info("Inserimento " + resTrue + " con verso true and " + resFalse + " con verso false");
        this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS))
                .contentType(MediaType.APPLICATION_JSON).content(resTrueJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS))
                .contentType(MediaType.APPLICATION_JSON).content(resFalseJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        logger.info("Prenotazioni inserite correttamente!");

        logger.info("Controllo reservation con verso true ...");
        MvcResult result = this.mockMvc.perform(get("/reservations/verso/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS) + "/true")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        GetReservationsNomeLineaDataVersoResource resource = objectMapper.readValue(result.getResponse().getContentAsString(), GetReservationsNomeLineaDataVersoResource.class);
        //controllo che ci sia il bimbo prenotato alla fermata, verso, data giusta
        assert (resource.getAlunniPerFermata().stream().filter(fermataDTOAlunni -> fermataDTOAlunni.getFermata().getId().equals(lineaEntity.getAndata().get(0))).collect(Collectors.toList()).get(0).getAlunni().stream().filter(alunni -> alunni.getCodiceFiscale().equals(childMap.get(1).getCodiceFiscale())).count() == 1);

//  TODO  Eliminare se si considera sufficiente testare il verso di andata

//        logger.info("Controllo reservation with towards false ...");
//        result = this.mockMvc.perform(get("/reservations/verso/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS) + "/false")
//                .contentType(MediaType.APPLICATION_JSON)
//                .header("Authorization", "Bearer " + token))
//                .andExpect(status().isOk())
//                .andReturn();
//        resource = objectMapper.readValue(result.getResponse().getContentAsString(), GetReservationsNomeLineaDataVersoResource.class);
//        assert (resource.getAlunniPerFermata().stream().filter(fermataDTOAlunni -> fermataDTOAlunni.getFermata().getId().equals(lineaEntity.getRitorno().get(0))).collect(Collectors.toList()).get(0).getAlunni().stream().filter(alunni -> alunni.getCodiceFiscale().equals(childMap.get(1).getCodiceFiscale())).count() == 1);

        logger.info("PASSED");
    }


    /**
     * Controlla GET /notreservations/{data}/{verso}.
     * Tale endPoint deve ritornare solo i bambini che non hanno una reservation per tale data e verso.
     * Viene effettuata solo una reservation per una data a @dayShift da oggi e verso andata.
     * Con verso=ritorno vogliamo che ritorni tutti i bambini del db.
     * Con verso=andata, vogliamo la lista dei bambini precedentemente letta priva del bambino per cui abbiamo prenotato con verso andata.
     *
     * @throws Exception
     */
    @Test
    public void getNotReservations() throws Exception {
        String token = loginAsSystemAdmin();
        int dayShift = 1000; //per essere certi che ci sia solo la prenotazione di test che inseriamo
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);
        ReservationResource res = ReservationResource.builder().cfChild(childMap.get(1).getCodiceFiscale()).idFermata(lineaEntity.getAndata().get(0)).verso(true).build();
        String resTrueJson = objectMapper.writeValueAsString(res);

        logger.info("Inserimento reservation " + res + " andata ...");
        MvcResult result1 = this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(dayShift, ChronoUnit.DAYS))
                .contentType(MediaType.APPLICATION_JSON).content(resTrueJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        logger.info("Inserito correttamente!");

//      TODO eliminare, ho pensato non fosse necessario usare un endpoint http, ma si può passare direttamente dal db
// todo probabilmente si può anche cancellare l'endpoint

//        logger.info("Lettura di tutti i bambini iscritti...");
//        MvcResult result1Child = this.mockMvc.perform(get("/admin/children/")
//                .contentType(MediaType.APPLICATION_JSON).content(resTrueJson)
//                .header("Authorization", "Bearer " + token))
//                .andExpect(status().isOk())
//                .andReturn();
//        String resChildren = result1Child.getResponse().getContentAsString();
//
//        List<ChildDTO> allChildList = objectMapper.readValue(resChildren, new TypeReference<List<ChildDTO>>() {
//        });
//        logger.info("Lettura bambini eseguita!");

        List<ChildDTO> allChildList = childRepository.findAll().stream().map(ChildDTO::new).collect(Collectors.toList());

        //tutti i bambini non devono essere prenotati per il ritorno
        logger.info("Controllo bambini non prenotati ritorno...");
        this.mockMvc.perform(get("/notreservations/" + LocalDate.now().plus(dayShift, ChronoUnit.DAYS) + "/false")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(allChildList)));
        logger.info("Corretto");


        logger.info("Controllo bambini non prenotati andata...");
        //ci devono essere tutti i bambini tranne quello per cui abbiamo prenotato
        List<ChildDTO> childListWithoutBooked = allChildList.stream().filter(childDTO -> !childDTO.getCodiceFiscale().equals(childMap.get(1).getCodiceFiscale())).collect(Collectors.toList());

        this.mockMvc.perform(get("/notreservations/" + LocalDate.now().plus(dayShift, ChronoUnit.DAYS) + "/true")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(objectMapper.writeValueAsString(childListWithoutBooked)));

        logger.info("PASSED");
    }


    /**
     * Controlla PUT /reservations/{nome_linea}/{data}/{reservation_id}
     *
     * @throws Exception
     */
    @Test
    public void putReservation() throws Exception {
        String token = loginAsSystemAdmin();
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);
        ReservationResource res = ReservationResource.builder().cfChild(childMap.get(1).getCodiceFiscale()).idFermata(lineaEntity.getAndata().get(0)).verso(true).build();
        String resJson = objectMapper.writeValueAsString(res);

        logger.info("Inserimento e controllo posizione " + res + "...");
        MvcResult result1 = this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS))
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();

        String idRes = objectMapper.readValue(result1.getResponse().getContentAsString(), String.class);

        logger.info("Modifico Reservation ...");
        ReservationResource resUpdated = ReservationResource.builder().cfChild(childMap.get(1).getCodiceFiscale()).idFermata(lineaEntity.getRitorno().get(0)).verso(false).build();
        String resCorrectJson = objectMapper.writeValueAsString(resUpdated);

        this.mockMvc.perform(put("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS) + "/" + idRes)
                .contentType(MediaType.APPLICATION_JSON).content(resCorrectJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        Optional<ReservationEntity> reservationEntityCheck = reservationRepository.findById(new ObjectId(idRes));
        assert reservationEntityCheck.isPresent();
        assert reservationEntityCheck.get().equalsResource(resUpdated);

        logger.info("PASSED");
    }

    /**
     * Controlla PUT /reservations/{nome_linea}/{data}/{reservation_id} con verso/id fermata non congruente
     *
     * @throws Exception
     */
    @Test
    public void putReservation_Wrong() throws Exception {
        String token = loginAsSystemAdmin();
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);
        ReservationResource res = ReservationResource.builder().cfChild(childMap.get(1).getCodiceFiscale()).idFermata(lineaEntity.getAndata().get(0)).verso(true).build();
        String resJson = objectMapper.writeValueAsString(res);

        logger.info("Inserimento " + res + "...");

        MvcResult result1 = this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS))
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String idRes = objectMapper.readValue(result1.getResponse().getContentAsString(), String.class);

        logger.info("Inserito correttamente!");

        logger.info("Modifico Reservation in modo errato");
        ReservationResource resWrong = ReservationResource.builder().cfChild(childMap.get(1).getCodiceFiscale()).idFermata(lineaEntity.getRitorno().get(0)).verso(true).build();
        String resWrongJson = objectMapper.writeValueAsString(resWrong);

        this.mockMvc.perform(put("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS) + "/" + idRes)
                .contentType(MediaType.APPLICATION_JSON).content(resWrongJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());
        logger.info("PASSED");
    }


    /**
     * Controlla DELETE /reservations/{nome_linea}/{data}/{reservation_id} con il reservation_id random/non congruente
     * todo (marcof) non capisco perchè testiamo che non si possa cancellare un objectId a caso, se casualmente prendiamo quello di una prenotazione dovremmo poterla cancellare, anche il numero a caso mi lascia un po' perplesso
     *
     * @throws Exception
     */
    @Test
    public void deleteReservation_randomID() throws Exception {
        String token = loginAsSystemAdmin();

        LineaEntity lineaEntity = lineaRepository.findAll().get(0);
        logger.info("Cancellazione a caso errata con numero...");
        this.mockMvc.perform(delete("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(4, ChronoUnit.DAYS) + "/12345")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        logger.info("Cancellazione a caso errata con objectID...");
        this.mockMvc.perform(delete("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(1000, ChronoUnit.DAYS) + "/" + new ObjectId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());
        logger.info("DONE");
    }

    /**
     * Test dei permessi di un genitore per post, put e delete di una prenotazione
     *
     * @throws Exception
     */
    @Test
    public void reservation_PermissionGenitore() throws Exception {
        logger.info("Test inserimento reservation per un proprio figlio");
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);

        String json = objectMapper.writeValueAsString(userDTOMap.get("testGenitore"));
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

        String idRes = inserimentoReservationGenitore();

        logger.info("Test modifica reservation per un proprio figlio");
        ReservationResource resCorrect = ReservationResource.builder().cfChild(userEntityMap.get("testGenitore").getChildrenList().iterator().next()).idFermata(2).verso(true).build();
        String resCorrectJson = objectMapper.writeValueAsString(resCorrect);
        MvcResult result1 = this.mockMvc.perform(put("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(1, ChronoUnit.DAYS) + "/" + idRes)
                .contentType(MediaType.APPLICATION_JSON).content(resCorrectJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();


        logger.info("Test delete reservation per un proprio figlio");
        this.mockMvc.perform(delete("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(1, ChronoUnit.DAYS) + "/" + idRes)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());


    }

    /**
     * Test dei permessi di un non genitore per post, put e delete di una prenotazione
     * non genitore = genitore che prova a fare cose per un figlio di qualcun altro
     *
     * @throws Exception
     */
    @Test
    public void reservation_PermissionNonGenitore() throws Exception {
        //creazione reservation valida
        String idRes = inserimentoReservationGenitore();
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);

        logger.info("Test inserimento reservation per un figlio altrui");

        String json = objectMapper.writeValueAsString(userDTOMap.get("testNonGenitore"));
        MvcResult result = this.mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

        ReservationResource res = ReservationResource.builder().cfChild(userEntityMap.get("testGenitore").getChildrenList().iterator().next()).idFermata(1).verso(true).build();
        String resJson = objectMapper.writeValueAsString(res);
        logger.info("Inserimento reservation: " + res);
        this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(2, ChronoUnit.DAYS) + "/")
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError()).andReturn();

        logger.info("Test modifica reservation per un figlio altrui");
        ReservationResource resCorrect = ReservationResource.builder().cfChild(userEntityMap.get("testGenitore").getChildrenList().iterator().next()).idFermata(1).verso(true).build();
        String resCorrectJson = objectMapper.writeValueAsString(resCorrect);
        this.mockMvc.perform(put("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(1, ChronoUnit.DAYS) + "/" + idRes)
                .contentType(MediaType.APPLICATION_JSON).content(resCorrectJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());

        logger.info("Test delete reservation per un figlio altrui");
        this.mockMvc.perform(delete("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(1, ChronoUnit.DAYS) + "/" + idRes)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());
    }

    /**
     * Test dei permessi di un nonno per post, put e delete di una prenotazione
     * Un nonno con ruolo admin può prenotare solo per il giorno stesso
     *
     * @throws Exception
     */
    @Test
    public void reservation_PermissionNonno() throws Exception {
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);

        String json = objectMapper.writeValueAsString(userDTOMap.get("testNonno"));
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        ReservationResource res = ReservationResource.builder().cfChild(userEntityMap.get("testGenitore").getChildrenList().iterator().next()).idFermata(1).verso(true).build();
        String resJson = objectMapper.writeValueAsString(res);

        logger.info("Inserimento " + res);
        MvcResult result1 = this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now())
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        String idRes = objectMapper.readValue(result1.getResponse().getContentAsString(), String.class);


        logger.info("Test modifica reservation per nonno");
        ReservationResource resCorrect = ReservationResource.builder().cfChild(userEntityMap.get("testGenitore").getChildrenList().iterator().next()).idFermata(1).verso(true).build();
        String resCorrectJson = objectMapper.writeValueAsString(resCorrect);
        this.mockMvc.perform(put("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now() + "/" + idRes)
                .contentType(MediaType.APPLICATION_JSON).content(resCorrectJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        logger.info("Test delete reservation per nonno");
        this.mockMvc.perform(delete("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now() + "/" + idRes)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

    }


    private String inserimentoReservationGenitore() throws Exception {
        LineaEntity lineaEntity = lineaRepository.findAll().get(0);

        String json = objectMapper.writeValueAsString(userDTOMap.get("testGenitore"));
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        ReservationResource res = ReservationResource.builder().cfChild(userEntityMap.get("testGenitore").getChildrenList().iterator().next()).idFermata(1).verso(true).build();
        String resJson = objectMapper.writeValueAsString(res);

        logger.info("Inserimento " + res);
        MvcResult result1 = this.mockMvc.perform(post("/reservations/" + lineaEntity.getId() + "/" + LocalDate.now().plus(1, ChronoUnit.DAYS) + "/")
                .contentType(MediaType.APPLICATION_JSON).content(resJson)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readValue(result1.getResponse().getContentAsString(), String.class);

    }

    private String loginAsSystemAdmin() throws Exception {

        UserDTO user = new UserDTO();
        user.setEmail(superAdminMail);
        user.setPassword(superAdminPass);
        String json = objectMapper.writeValueAsString(user);
        MvcResult result = this.mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("token").asText();
    }
}
