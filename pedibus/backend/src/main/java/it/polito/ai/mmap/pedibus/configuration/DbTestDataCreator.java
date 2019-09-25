package it.polito.ai.mmap.pedibus.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polito.ai.mmap.pedibus.entity.*;
import it.polito.ai.mmap.pedibus.objectDTO.UserDTO;
import it.polito.ai.mmap.pedibus.repository.*;
import it.polito.ai.mmap.pedibus.services.MongoZonedDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import javax.annotation.PostConstruct;
import javax.management.relation.Role;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DbTestDataCreator {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    ChildRepository childRepository;
    @Autowired
    LineaRepository lineaRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ReservationRepository reservationRepository;
    @Autowired
    FermataRepository fermataRepository;

    @Autowired
    private Environment environment;

    /**
     * crea:
     * - 100 Child
     * - 50 genitori con 2 figli        contenuti nel file genitori.json e pw = 1!qwerty1!
     * - 25 nonni GUIDE della prima linea    contenuti nel file nonni_0.json e pw = 1!qwerty1! i primi 5 sono anche admin
     * - 25 nonni GUIDE della seconda linea    contenuti nel file nonni_1.json e pw = 1!qwerty1! i primi 5 sono anche admin
     * - 1 reservation/figlio per oggi, domani e dopo domani (andata e ritorno)
     */
    public void makeChildUserReservations() throws IOException {
        reservationRepository.deleteAll();
        userRepository.deleteAll();
        childRepository.deleteAll();
        int count = 0;
        RoleEntity roleUser = roleRepository.findByRole("ROLE_USER");
        RoleEntity roleAdmin = roleRepository.findByRole("ROLE_ADMIN");
        RoleEntity roleGuide = roleRepository.findByRole("ROLE_GUIDE");
        List<LineaEntity> lineaEntityList = lineaRepository.findAll();


        List<ChildEntity> childList = objectMapper.readValue(ResourceUtils.getFile("classpath:debug_container/childEntity.json"), new TypeReference<List<ChildEntity>>() {
        });

        List<UserEntity> userList = userEntityListConverter("genitori.json", roleUser);
        Iterator<ChildEntity> childEntityIterable = childList.iterator();

        int i = 0;
        while (childEntityIterable.hasNext()) {
            ChildEntity child1 = childEntityIterable.next();
            ChildEntity child2 = childEntityIterable.next();
            UserEntity parent = userList.get(i);
            parent.setChildrenList(new HashSet<>(Arrays.asList(child1.getCodiceFiscale(), child2.getCodiceFiscale())));
            parent.setEnabled(true);

            parent = userRepository.save(parent); //per avere l'objectId

            userList.set(i, parent);
            child1.setIdParent(parent.getId());
            child2.setIdParent(parent.getId());

            i++;
        }
        childRepository.saveAll(childList);
        logger.info("Tutti i genitori e i figli caricati");

        LinkedList<UserEntity> listNonni = new LinkedList<>();
        for (i = 0; i <= 1; i++) {
            count = 0;
            for (UserEntity nonno : userEntityListConverter("nonni_" + i + ".json", roleGuide)) {
                nonno.setEnabled(true);

                if (count < 5) {
                    LineaEntity lineaEntity = lineaEntityList.get(i);
                    nonno.getRoleList().add(roleAdmin);
                    lineaEntity.getAdminList().add(nonno.getUsername());
                    lineaRepository.save(lineaEntity);
                }
                listNonni.add(nonno);

                count++;
            }
        }

        userRepository.saveAll(listNonni);
        logger.info(count + " nonni caricati");

        i = 0;
        count = 0;
        List<ReservationEntity> reservationsList = new LinkedList<>();
        ReservationEntity reservationEntity;
        int randLinea;
        for (int day = 0; day < 3; day++) {
            childEntityIterable = childList.iterator();
            while (childEntityIterable.hasNext()) {
                ChildEntity childEntity = childEntityIterable.next();

                //andata
                reservationEntity = new ReservationEntity();
                reservationEntity.setCfChild(childEntity.getCodiceFiscale());
                reservationEntity.setData(MongoZonedDateTime.getMongoZonedDateTimeFromDate(LocalDate.now().plus(day, ChronoUnit.DAYS).toString()));
                reservationEntity.setIdLinea(fermataRepository.findById(childEntity.getIdFermataAndata()).get().getIdLinea());
                reservationEntity.setVerso(true);
                reservationEntity.setIdFermata(childEntity.getIdFermataAndata());
                if (!reservationRepository.findByCfChildAndData(reservationEntity.getCfChild(), reservationEntity.getData()).isPresent()) {
                    reservationsList.add(reservationEntity);
                    count++;
                }

                //ritorno
                reservationEntity = new ReservationEntity();
                reservationEntity.setCfChild(childEntity.getCodiceFiscale());
                reservationEntity.setData(MongoZonedDateTime.getMongoZonedDateTimeFromDate(LocalDate.now().plus(day, ChronoUnit.DAYS).toString()));
                reservationEntity.setIdLinea(fermataRepository.findById(childEntity.getIdFermataRitorno()).get().getIdLinea());
                reservationEntity.setVerso(false);
                reservationEntity.setIdFermata(childEntity.getIdFermataRitorno());

                if (!reservationRepository.findByCfChildAndData(reservationEntity.getCfChild(), reservationEntity.getData()).isPresent()) {
                    reservationsList.add(reservationEntity);
                    count++;
                }
                i++;
            }
        }

        reservationRepository.saveAll(reservationsList);
        logger.info(count + " reservations per oggi, domani e dopodomani caricate");
    }


    private List<UserEntity> userEntityListConverter(String fileName, RoleEntity roleEntity) throws IOException {

        List<UserDTO> userList = objectMapper.readValue(ResourceUtils.getFile("classpath:debug_container/" + fileName), new TypeReference<List<UserDTO>>() {
        });

        return userList.stream().map(userDTO -> new UserEntity(userDTO, new HashSet<>(Arrays.asList(roleEntity)), passwordEncoder)).collect(Collectors.toList());
    }
}
