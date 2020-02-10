package it.polito.ai.mmap.pedibus.services;


import it.polito.ai.mmap.pedibus.configuration.PedibusString;
import it.polito.ai.mmap.pedibus.entity.ChildEntity;
import it.polito.ai.mmap.pedibus.entity.ReservationEntity;
import it.polito.ai.mmap.pedibus.entity.UserEntity;
import it.polito.ai.mmap.pedibus.exception.ChildAlreadyPresentException;
import it.polito.ai.mmap.pedibus.exception.ChildNotFoundException;
import it.polito.ai.mmap.pedibus.exception.ReservationNotValidException;
import it.polito.ai.mmap.pedibus.objectDTO.ChildDTO;
import it.polito.ai.mmap.pedibus.objectDTO.ReservationDTO;
import it.polito.ai.mmap.pedibus.repository.ChildRepository;
import it.polito.ai.mmap.pedibus.resources.ChildDefaultStopResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChildService {
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    UserService userService;
    @Autowired
    MongoTimeService mongoTimeService;
    @Autowired
    ChildRepository childRepository;
    @Autowired
    LineeService lineeService;
    @Autowired
    ReservationService reservationService;
    @Autowired
    private NotificheService notificheService;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Value("${dataInizioScuola}")
    private String dataInizioScuola;


    public static String fromKeywordToRegex(String keyword) {
        List<String> keywords = Arrays.asList(keyword.split("\\s+"));
        StringBuilder regex = new StringBuilder("");
        for (int i = 0; i < keywords.size(); i++) {
            regex.append(".*").append(keywords.get(i)).append(".*");
            if (i != keywords.size() - 1) regex.append("|");
        }
        return regex.toString();
    }

    /**
     * Ottieni il bambino dal database
     *
     * @param codiceFiscale codicefiscale bambino
     * @return ChildDTO
     */
    public ChildDTO getChildDTOById(String codiceFiscale) {
        Optional<ChildEntity> checkChild = childRepository.findById(codiceFiscale);
        if (checkChild.isPresent())
            return new ChildDTO(checkChild.get());
        else
            throw new ChildNotFoundException(codiceFiscale);
    }

    public List<ChildDTO> getAllChildren() {
        return childRepository.findAll().stream().map(ChildDTO::new).collect(Collectors.toList());
    }

    /**
     * Recuperiamo da db i figli dell'utente loggato
     *
     * @return Lista dei figli del principal
     */
    public List<ChildDTO> getMyChildren() {
        UserEntity principal = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ((List<ChildEntity>) childRepository.findAllById(principal.getChildrenList())).stream().map(ChildDTO::new).collect(Collectors.toList());
    }

    /**
     * Metodo che permette di cambiare la fermata di default di un bambino o dal suo genitore o da un System-Admin
     *
     * @param cfChildNew codice fiscale del bambino
     * @param cfChildOld
     * @param stopRes    informazioni sulla nuova fermata da considerare
     * @param date       data di partenza da cui iniziare le modifiche
     */
    public void updateChildStop(String cfChildNew, String cfChildOld, ChildDefaultStopResource stopRes, Date date) {
        Optional<ChildEntity> c = childRepository.findById(cfChildNew);
        if (c.isPresent()) {
            UserEntity principal = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal.getChildrenList().contains(cfChildNew) || principal.getRoleList().contains(userService.getRoleEntityById("ROLE_SYSTEM-ADMIN"))) {
                ChildEntity childEntity = c.get();

//                lineeService.getFermataEntityById(stopRes.getIdFermataAndata());
                childEntity.setIdFermataAndata(stopRes.getIdFermataAndata());
//                lineeService.getFermataEntityById(stopRes.getIdFermataRitorno());
                childEntity.setIdFermataRitorno(stopRes.getIdFermataRitorno());

                childRepository.save(childEntity);
                // Remove one day todo spostare o riutilizzare cose in mongoTimeService
                LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).minusDays(1);
                Date out = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
                List<UserEntity> parents = this.getChildParents(cfChildNew);
                for (UserEntity parent : parents) {
                    this.simpMessagingTemplate.convertAndSendToUser(parent.getUsername(), "/child/" + childEntity.getCodiceFiscale(), childEntity);
                }

                Optional<List<ReservationEntity>> toBeUpdatedCheck = reservationService.reservationRepository.findByCfChildAndDataIsAfter(cfChildOld, out);
                List<ReservationEntity> toBeUpdated = toBeUpdatedCheck.orElseGet(ArrayList::new);

                // Controllo validità di singolo aggiornamento, gli altri andranno bene per forza
                if (toBeUpdated.isEmpty()) {
                    return;
                }
                ReservationDTO newReservationDTOCheck = new ReservationDTO(toBeUpdated.get(0));
                Integer fermataAndata = stopRes.getIdFermataAndata();
                Integer fermataRitorno = stopRes.getIdFermataRitorno();
                String lineaAndata = lineeService.getFermataEntityById(stopRes.getIdFermataAndata()).getIdLinea();
                String lineaRitorno = lineeService.getFermataEntityById(stopRes.getIdFermataRitorno()).getIdLinea();
                newReservationDTOCheck.setIdFermata(newReservationDTOCheck.getVerso() ? fermataAndata : fermataRitorno);
                newReservationDTOCheck.setIdLinea(newReservationDTOCheck.getVerso() ? lineaAndata : lineaRitorno);
                newReservationDTOCheck.setCfChild(cfChildNew);
                if (!this.reservationService.isValidReservation(newReservationDTOCheck)) {
                    throw new ReservationNotValidException(PedibusString.UPDATE_RESERVATION_NOT_VALID);
                }

                for (ReservationEntity res : toBeUpdated) {
                    // Cancellazione vecchia prenotazione
                    ReservationDTO oldreservationDTO = new ReservationDTO(res);

                    res.setIdFermata(oldreservationDTO.getVerso() ? fermataAndata : fermataRitorno);
                    res.setIdLinea(oldreservationDTO.getVerso() ? lineaAndata : lineaRitorno);
                    res.setCfChild(cfChildNew);
                    ReservationDTO newReservationDTO = new ReservationDTO(res);
                    this.notificheService.sendReservationNotification(oldreservationDTO, true);
                    this.notificheService.sendReservationNotification(newReservationDTO, false);
                }
                this.reservationService.reservationRepository.saveAll(toBeUpdated);
            } else
                throw new ChildNotFoundException(cfChildNew);
        } else
            throw new ChildNotFoundException(cfChildNew);
    }

    /**
     * Metodo da usare in altri service in modo da non dover fare sempre i controlli
     *
     * @param cfChild
     * @return
     */
    public ChildEntity getChildrenEntity(String cfChild) {
        Optional<ChildEntity> checkChild = childRepository.findById(cfChild);
        if (checkChild.isPresent()) {
            return checkChild.get();
        } else {
            throw new ChildNotFoundException(cfChild);
        }
    }

    /**
     * Restituisce i bambini iscritti tranne quelli che si sono prenotati per quel giorno linea e verso
     *
     * @param data
     * @param verso
     * @return
     */
    public List<ChildDTO> getChildrenNotReserved(List<String> childrenDataVerso, Date data, boolean verso) {
        List<String> childrenAll = childRepository.findAll().stream().map(ChildEntity::getCodiceFiscale).collect(Collectors.toList());
        List<String> childrenNotReserved = childrenAll.stream().filter(bambino -> !childrenDataVerso.contains(bambino)).collect(Collectors.toList());

        return childrenNotReserved.stream().map(this::getChildDTOById).collect(Collectors.toList());
    }

    public HashMap<String, ChildEntity> getChildrenEntityByCfList(Set<String> cfList) {
        return (HashMap<String, ChildEntity>) ((List<ChildEntity>) childRepository
                .findAllById(cfList)).stream().collect(Collectors.toMap(ChildEntity::getCodiceFiscale, c -> c));
    }

    /**
     * Ottieni lista parenti dato il codice fiscale del bambino
     *
     * @param cfChild codice fiscale bambino
     * @return Lista di parenti
     */
    public List<UserEntity> getChildParents(String cfChild) {
        return this.userService.userRepository.findAllByChildrenListIsContaining(cfChild);
    }

    /**
     * Restituisce tutti i bambini paginati
     *
     * @param pageable oggetto Pageable che contiene query per paginazione
     * @param keyword  Elenco di keyword di ricerca separate da spazi
     * @return Pagina bambini richiesta
     */
    public Page<ChildDTO> getAllPagedChildren(Pageable pageable, String keyword) {
        Page<ChildEntity> pagedUsersEntity = childRepository.searchByNameSurnameCF(ChildService.fromKeywordToRegex(keyword), pageable);
        return PageableExecutionUtils.getPage(pagedUsersEntity.stream()
                .map(ChildDTO::new)
                .collect(Collectors.toList()), pageable, pagedUsersEntity::getTotalElements);
    }

    /**
     * Creazione di un bambino
     *
     * @param childDTO dati del bambino
     * @return ChildEntity creato nel database
     */
    public ChildEntity createChild(ChildDTO childDTO) {
        if (childRepository.findById(childDTO.getCodiceFiscale()).isPresent()) {
            throw new ChildAlreadyPresentException(childDTO.getCodiceFiscale());
        }
        ChildEntity child = new ChildEntity(childDTO);
        ChildEntity childEntity = childRepository.save(child);
        this.reservationService.bulkReservation(childEntity);
        this.notificheService.sendUpdateNotification();
        return childEntity;
    }

    /**
     * Funzione di aggiornamento bambino
     *
     * @param childId  id del bambino da modificare
     * @param childDTO dati del bambino aggiornati
     * @return ChildEntity ottenuta
     */
    public ChildEntity updateChild(String childId, ChildDTO childDTO) {
        if (!childId.equals(childDTO.getCodiceFiscale()) && childRepository.findById(childDTO.getCodiceFiscale()).isPresent()) {
            throw new ChildAlreadyPresentException(childDTO.getCodiceFiscale());
        }
        Optional<ChildEntity> childEntityOptional = childRepository.findById(childId);
        if (!childEntityOptional.isPresent()) {
            throw new ChildNotFoundException(childId);
        }
        if (!childId.equals(childDTO.getCodiceFiscale())) {
            List<UserEntity> parentsOfChild = this.userService.userRepository.findAllByChildrenListIsContaining(childId);
            if (!parentsOfChild.isEmpty()) {
                this.deleteChildFromParent(childId, parentsOfChild);
                this.addChildToParent(childDTO, parentsOfChild);
                this.userService.userRepository.saveAll(parentsOfChild);
            }
        }
        ChildEntity childEntityOld = childEntityOptional.get();
        ChildEntity childEntityNew = new ChildEntity(childDTO);
        childRepository.delete(childEntityOld);
        ChildEntity result = childRepository.save(childEntityNew);
        ChildDefaultStopResource defaultStopResource = new ChildDefaultStopResource(result.getIdFermataAndata(), result.getIdFermataRitorno(), MongoTimeService.getNow().toString());
        this.updateChildStop(result.getCodiceFiscale(), childId, defaultStopResource, MongoTimeService.getMongoZonedDateTimeFromDateTimeString(dataInizioScuola, "05:00"));
        this.notificheService.sendUpdateNotification();
        return result;
    }

    /**
     * Aggiunta bambino ai genitori indicati
     *
     * @param childDTO       DTO del bambino da inserire
     * @param parentsOfChild parenti del bambino
     */
    private void addChildToParent(ChildDTO childDTO, List<UserEntity> parentsOfChild) {
        for (UserEntity u : parentsOfChild) {
            u.getChildrenList().add(childDTO.getCodiceFiscale());
        }
    }

    /**
     * Cancella il bambino dai genitori indicati
     *
     * @param childId        codice fiscale dal bambino da eliminare
     * @param parentsOfChild parenti del bambino
     */
    private void deleteChildFromParent(String childId, List<UserEntity> parentsOfChild) {
        for (UserEntity u : parentsOfChild) {
            u.getChildrenList().remove(childId);
        }

    }

    /**
     * Cancellazione del bambino dal database e relative prenotazioni
     *
     * @param childId codice fiscale
     */
    public void deleteChild(String childId) {
        Optional<ChildEntity> childEntityOptional = this.childRepository.findById(childId);
        if (!childEntityOptional.isPresent()) {
            throw new ChildNotFoundException(childId);
        }
        this.childRepository.delete(childEntityOptional.get());
        List<UserEntity> parentsOfChild = this.userService.userRepository.findAllByChildrenListIsContaining(childId);
        if (!parentsOfChild.isEmpty()) {
            this.deleteChildFromParent(childId, parentsOfChild);
            this.userService.userRepository.saveAll(parentsOfChild);
        }
        this.reservationService.deleteAllChildReservation(childId);
        this.notificheService.sendUpdateNotification();
    }
}
