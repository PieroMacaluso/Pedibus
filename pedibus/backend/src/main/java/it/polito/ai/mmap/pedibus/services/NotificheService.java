package it.polito.ai.mmap.pedibus.services;

import it.polito.ai.mmap.pedibus.entity.NotificaEntity;
import it.polito.ai.mmap.pedibus.entity.UserEntity;
import it.polito.ai.mmap.pedibus.exception.NotificaNotFoundException;
import it.polito.ai.mmap.pedibus.exception.NotificaWrongTypeException;
import it.polito.ai.mmap.pedibus.exception.UserNotFoundException;
import it.polito.ai.mmap.pedibus.objectDTO.NotificaDTO;
import it.polito.ai.mmap.pedibus.repository.NotificaRepository;
import it.polito.ai.mmap.pedibus.repository.UserRepository;
import lombok.Data;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Data

public class NotificheService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    NotificaRepository notificaRepository;

    @Value("${notifiche.type.Base}")
    String NotBASE;
    @Value("${notifiche.type.Disponibilita}")
    String NotDISPONIBILITA;

    @Autowired
    UserRepository userRepository;


    /**
     * Restituisce le notifiche non lette con type 'base' dell'utente
     *
     * @param username
     * @return
     */
    public ArrayList<NotificaDTO> getNotificheBase(String username) {
        Optional<UserEntity> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            List<NotificaEntity> notifiche = notificaRepository.findAll().stream().filter(notificaEntity -> notificaEntity.getUsernameDestinatario().equals(username)).filter(notificaEntity -> notificaEntity.getType().compareTo(NotBASE) == 0).filter(notificaEntity -> !notificaEntity.getIsTouched()).collect(Collectors.toList());
            ArrayList<NotificaDTO> notificheDTO = new ArrayList<>();

            for (NotificaEntity n : notifiche) {
                notificheDTO.add(new NotificaDTO(n));
            }

            return notificheDTO;
        } else {
            throw new UserNotFoundException();
        }
    }

    /**
     * Restituisce le notifiche non lette e non ack con type 'disponibilita' dell'utente
     *
     * @param username
     * @return
     */
    public ArrayList<NotificaDTO> getNotificheDisponibilita(String username) {
        Optional<UserEntity> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            List<NotificaEntity> notifiche = notificaRepository.findAll().stream().filter(notificaEntity -> notificaEntity.getUsernameDestinatario().equals(username)).filter(notificaEntity -> notificaEntity.getType().compareTo(NotDISPONIBILITA) == 0).filter(notificaEntity -> !notificaEntity.getIsTouched()).filter(notificaEntity -> !notificaEntity.getIsAck()).collect(Collectors.toList());
            ArrayList<NotificaDTO> notificheDTO = new ArrayList<>();

            for (NotificaEntity n : notifiche) {
                notificheDTO.add(new NotificaDTO(n));
            }

            return notificheDTO;
        } else {
            throw new UserNotFoundException();
        }
    }

    /**
     * Restituisce tutte le notifiche non lette dell'utente
     *
     * @param username
     * @return
     */
    public ArrayList<NotificaDTO> getNotifiche(String username) {
        Optional<UserEntity> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            List<NotificaEntity> notifiche = notificaRepository.findAll().stream().filter(notificaEntity -> notificaEntity.getUsernameDestinatario().equals(username)).filter(notificaEntity -> !notificaEntity.getIsTouched()).filter(notificaEntity -> !notificaEntity.getIsAck()).collect(Collectors.toList());
            ArrayList<NotificaDTO> notificheDTO = new ArrayList<>();

            for (NotificaEntity n : notifiche) {
                notificheDTO.add(new NotificaDTO(n));
            }

            return notificheDTO;
        } else {
            throw new UserNotFoundException();
        }
    }


    /**
     * Elimina una qualsiasi notifica attraverso il suo id
     *
     * @param idNotifica
     */
    public void deleteNotifica(String idNotifica) {
        Optional<NotificaEntity> checkNotificaEntity = notificaRepository.findById(idNotifica);
        if (checkNotificaEntity.isPresent()) {
            //idNotifica di una notifica base
            NotificaEntity notificaEntity = checkNotificaEntity.get();
            if (notificaEntity.getType().equals(NotBASE))
                notificaRepository.delete(notificaEntity);
        } else {
            throw new NotificaNotFoundException();
        }
    }

    /**
     * Salva nel db una nuova notifica passata come parametro
     */
    public void addNotifica(NotificaEntity notificaEntity) {
        notificaRepository.save(notificaEntity);
        logger.info("Notifica salvata.");
    }

}
