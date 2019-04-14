package it.polito.ai.mmap.esercitazione2.controller;

import it.polito.ai.mmap.esercitazione2.entity.CompositeKeyPrenotazione;
import it.polito.ai.mmap.esercitazione2.objectDTO.PrenotazioneDTO;
import it.polito.ai.mmap.esercitazione2.resources.GetReservationsNomeLineaDataResource;
import it.polito.ai.mmap.esercitazione2.resources.LinesNomeLineaResource;
import it.polito.ai.mmap.esercitazione2.resources.LinesResource;
import it.polito.ai.mmap.esercitazione2.resources.PrenotazioneResource;
import it.polito.ai.mmap.esercitazione2.services.JsonHandlerService;
import it.polito.ai.mmap.esercitazione2.services.LineService;
import it.polito.ai.mmap.esercitazione2.services.MongoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

@RestController
public class HomeController {


    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    JsonHandlerService jsonHandlerService;
    @Autowired
    LineService lineService;
    @Autowired
    MongoService mongoService;

    /**
     * Metodo eseguito all'avvio della classe come init per leggere le linee del pedibus
     */
    @PostConstruct
    public void init() throws Exception {
        logger.info("Caricamento linee in corso...");
        jsonHandlerService.readPiedibusLines();             //todo verificare se possibile evitare se non ci sono modifiche
        logger.info("Caricamento linee completato.");
    }

    /**
     * Mapping verso la home dell'applicazione
     */
    @GetMapping("/")
    public String home() {
        return "home";
    }

    /**
     * Restituisce una JSON con una lista dei nomi delle lines presenti nel DBMS
     */
    @GetMapping("/lines")
    public LinesResource getLines() {
        return new LinesResource(lineService.getAllLines());
    }

    /**
     * Restituisce un oggetto JSON contenente due liste, riportanti i dettagli delle fermate di andata e ritorno
     */
    @GetMapping("/lines/{nome_linea}")
    public LinesNomeLineaResource getStopsLine(@PathVariable("nome_linea") String name) {
        LinesNomeLineaResource linesNomeLineaResource = new LinesNomeLineaResource(lineService.getLine(name));
        //linesNomeLineaResource.add(ControllerLinkBuilder.linkTo(HomeController.class).slash(jsonHandlerService).withSelfRel()); TODO capire l'utilità dei link

        return linesNomeLineaResource;
    }

    /**
     * Restituisce un oggetto JSON contenente due liste,riportanti, per ogni fermata di andata e ritorno, l’elenco delle
     * persone che devono essere prese in carico o lasciate in corrispondenza della fermata
     */
    @GetMapping("/reservation/{nome_linea}/{data}")
    public GetReservationsNomeLineaDataResource getReservations(@PathVariable("nome_linea") String nomeLinea, @PathVariable("data") String data) {

        return null;
    }

    /**
     * Invia un oggetto JSON contenente il nome dell’alunno da trasportare, l’identificatore della fermata a cui sale/scende e il verso di percorrenza (andata/ritorno);
     * TODO restituisce un identificatore univoco della prenotazione creata
     */
    @PostMapping("/reservations/{nome_linea}/{data}")
    public String postReservation(@RequestBody PrenotazioneResource prenotazioneResource, @PathVariable("nome_linea") String nomeLinea, @PathVariable("data") String data) {
        PrenotazioneDTO prenotazioneDTO = new PrenotazioneDTO(prenotazioneResource, lineService.getLine(nomeLinea), data);
        return mongoService.addPrenotazione(prenotazioneDTO).toString();
    }

    /**
     * Invia un oggetto JSON che permette di aggiornare i dati relativi alla prenotazione indicata
     * il reservation_id ci permette di identificare la prenotazione da modificare, il body contiene i dati aggiornati
     */
    @PutMapping("/reservations/{nome_linea}/{data}/{reservation_id}")
    public void updateReservation(@RequestBody PrenotazioneResource prenotazioneResource, @PathVariable("nome_linea") String nomeLinea, @PathVariable("data")  String data, @PathVariable("reservation_id") String reservationId) {
        PrenotazioneDTO prenotazioneDTO = new PrenotazioneDTO(prenotazioneResource, lineService.getLine(nomeLinea), data);
        CompositeKeyPrenotazione compositeKeyPrenotazione = new CompositeKeyPrenotazione(reservationId);
        mongoService.updatePrenotazione(prenotazioneDTO,compositeKeyPrenotazione);
    }

    /*
     * Elimina la prenotazione indicata
     *
     */
    @DeleteMapping("/reservations/{nome_linea}/{data}/{reservation_id}")
    public void deleteReservation(@RequestBody PrenotazioneResource prenotazioneResource, @PathVariable("nome_linea") String nomeLinea, @PathVariable("data") String data, @PathVariable("reservation_id") String reservationId) {
    }

    /*
     * Restituisce la prenotazione
     */
    @GetMapping("/reservations/{nome_linea}/{data}/{reservation_id}")
    public void getReservation(@RequestBody PrenotazioneResource prenotazioneResource, @PathVariable("nome_linea") String nomeLinea, @PathVariable("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data, @PathVariable("reservation_id") String reservationId) {
    }


}