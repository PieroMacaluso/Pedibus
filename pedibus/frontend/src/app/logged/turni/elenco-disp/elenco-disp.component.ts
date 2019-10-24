import {Component, OnInit} from '@angular/core';
import {SyncService} from '../../presenze/sync.service';
import {ApiService} from '../../api.service';
import {ApiDispService, DispAllResource} from '../../api-disp.service';
import {AuthService} from '../../../registration/auth.service';
import {MatDialog, MatSnackBar} from '@angular/material';
import {RxStompService} from '@stomp/ng2-stompjs';
import {DatePipe} from '@angular/common';
import {finalize, mergeMap, tap} from 'rxjs/operators';
import {defer, forkJoin, Observable, Subject} from 'rxjs';
import {PrenotazioneRequest, StopsByLine} from '../../line-details';
import {ApiTurniService, MapDisp, TurnoDispResource, TurnoResource} from '../../api-turni.service';
import {variable} from '@angular/compiler/src/output/output_ast';

@Component({
  selector: 'app-elenco-disp',
  templateUrl: './elenco-disp.component.html',
  styleUrls: ['./elenco-disp.component.scss']
})
export class ElencoDispComponent implements OnInit {

  idFermata: string;
  prenotazione$: Observable<PrenotazioneRequest>;
  turno$: Observable<TurnoDispResource>;
  private loading: boolean;
  private p: PrenotazioneRequest;
  private stops$: Observable<StopsByLine>;
  private changeDisp = new Subject<MapDisp>();
  private changeTurno = new Subject<TurnoResource>();

  private linea: StopsByLine;
  private turno: TurnoResource;
  private listDisp: MapDisp;


  constructor(private syncService: SyncService, private apiService: ApiService, private apiTurniService: ApiTurniService,
              private authService: AuthService, private dialog: MatDialog, private snackBar: MatSnackBar,
              private rxStompService: RxStompService, private datePipe: DatePipe) {

    this.syncService.prenotazioneObs$.pipe(
      tap(() => this.loading = true),
      mergeMap(
        pren => {
          this.p = pren;
          const stops = this.apiService.getStopsByLine(pren.linea);
          const turno = this.apiTurniService.getTurno(pren.linea, pren.verso, pren.data);
          return forkJoin([stops, turno]);
        }
      ),
      tap(() => this.loading = false)
    ).subscribe(
      res => {
        console.log(res);
        this.linea = res[0];
        res[1].turno.opening = false;
        res[1].turno.closing = false;
        this.changeTurno.next(res[1].turno);
        this.changeDisp.next(res[1].listDisp);
      },
      err => {
        // TODO: Errore
      }
    );

    // change Disp
    this.changeDisp.asObservable().subscribe(
      res => {
        this.listDisp = res;
      }
    );

    // change Turno
    this.changeTurno.asObservable().subscribe(
      res => {
        res.opening = false;
        res.closing = false;
        this.turno = res;
      }
    );
  }

  showLoading() {
    return this.loading;
  }

  ngOnInit() {
  }

  openTurno(turno: TurnoResource) {
    this.turno.opening = true;
    this.apiTurniService.setStateTurno(this.p.linea, this.p.verso, this.p.data, true).subscribe(response => {
      turno.isOpen = true;
      this.changeTurno.next(turno);
    }, (error) => {
      // TODO: errore
    });
  }

  closeTurno(turno: TurnoResource) {
    this.turno.closing = true;
    this.apiTurniService.setStateTurno(this.p.linea, this.p.verso, this.p.data, false).subscribe(response => {
      turno.isOpen = false;
      this.changeTurno.next(this.turno);
    }, (error) => {
      // TODO: errore
    });
  }

  statusTurno(checked: boolean) {
    this.turno.opening = true;
    this.apiTurniService.setStateTurno(this.p.linea, this.p.verso, this.p.data, checked).subscribe(response => {
      this.turno.isOpen = checked;
      this.changeTurno.next(this.turno);
    }, (error) => {
      // TODO: errore
    });
  }
}
