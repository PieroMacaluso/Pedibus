import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {SignInModel, SignUpModel} from './models';
import {shareReplay, tap} from 'rxjs/operators';
import * as moment from 'moment';
import * as jwt_decode from 'jwt-decode';
import {myRxStompConfig} from '../my-rx-stomp.config';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  baseURL = environment.baseURL;

  constructor(private httpClient: HttpClient) {
  }

  /* METODO CHE RINNOVA LE CREDENZIALI */
  postNewPassword(token: string, model) {
    return this.httpClient.post(this.baseURL + 'recover/' + token, model);
  }

  getConfirm(token: string) {
    return this.httpClient.get(this.baseURL + 'confirm/' + token);
  }

  postRecover(email: string) {
    return this.httpClient.post(this.baseURL + 'recover', email);
  }

  signUp(model: SignUpModel) {
    return this.httpClient.post(this.baseURL + 'register', model);
  }

  checkDuplicate(email: string) {
    return this.httpClient.get<boolean>(this.baseURL + 'register/checkMail/' + email);
  }

  signIn(model: SignInModel) {
    // We are calling shareReplay to prevent the receiver of this Observable from
    // accidentally triggering multiple POST requests due to multiple subscriptions.
    return this.httpClient.post(this.baseURL + 'login', model).pipe(tap(res => this.setSession(res)), shareReplay());
  }

  private setSession(authResult) {
    // TODO: aggiunto .jwtToken, da verificare che ci sia ancora in backend a fine es5
    console.log(JSON.stringify(jwt_decode(authResult.token)));
    const expiresAt = moment((jwt_decode(authResult.token).exp) * 1000);
    console.log('expires at: ' + expiresAt);

    localStorage.setItem('id_token', authResult.token);
    localStorage.setItem('roles', JSON.stringify(jwt_decode(authResult.token).roles));
    localStorage.setItem('expires_at', JSON.stringify(expiresAt.valueOf()));
    myRxStompConfig.connectHeaders = {
      Auth: authResult.token
    };
  }

  logout() {
    localStorage.removeItem('id_token');
    localStorage.removeItem('roles');
    localStorage.removeItem('expires_at');
  }

  isAdmin() {
    if (this.isLoggedIn()) {
      const roles = JSON.parse(localStorage.getItem('roles'));
      return roles.find(role => role === 'ROLE_SYSTEM-ADMIN') || roles.find(role => role === 'ROLE_ADMIN');
    }
    return false;
  }

  public isLoggedIn() {
    if (this.getExpiration() == null) {
      return false;
    } else {
      return moment().isBefore(this.getExpiration());
    }
  }

  getExpiration() {
    const expiration = localStorage.getItem('expires_at');
    if (expiration != null) {
      const expiresAt = JSON.parse(expiration);
      return moment(expiresAt);
    } else {
      return null;
    }

  }

}
