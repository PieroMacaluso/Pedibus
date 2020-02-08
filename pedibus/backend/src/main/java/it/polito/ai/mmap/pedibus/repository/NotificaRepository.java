package it.polito.ai.mmap.pedibus.repository;

import it.polito.ai.mmap.pedibus.entity.NotificaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface NotificaRepository extends MongoRepository<NotificaEntity, String> {
    void deleteAllByUsernameDestinatario(String username);
    Page<NotificaEntity> findAllByUsernameDestinatarioOrderByDataDesc(String user, Pageable pageable);
    List<NotificaEntity> findAllByUsernameDestinatarioAndIsAckAndIsTouchedAndAndType(String user, boolean isAck, boolean isTouched, NotificaEntity.NotificationType type);
}
