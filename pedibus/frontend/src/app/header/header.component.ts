import { Component, OnInit } from '@angular/core';
import {AuthService} from '../registration/auth.service';
import {Router} from '@angular/router';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent implements OnInit {

  opened: boolean;
  logo: any = '../../assets/svg/logo.svg';
  userLogged: boolean;
  loggedIcon = ['', '', ''];
  loggedLinks = ['', '', ''];
  loggedTitle = ['', '', ''];

  notLoggedIcon = ['vpn_key', 'person_add'];
  notLoggedLinks = ['sign-in', 'sign-up'];
  notLoggedTitle = ['Sign In', 'Sign Up'];
  activeLoggedLink: any;
  activeNotLoggedLink: any;

  constructor(private auth: AuthService, private router: Router) {
    this.userLogged = this.auth.isLoggedIn();
    this.activeLoggedLink = this.loggedLinks[0];
    this.activeNotLoggedLink = this.notLoggedLinks[0];
    this.setLinks();
  }

  isLoggedIn() {
    return this.auth.isLoggedIn();
  }

  logOut() {
    this.userLogged = false;
    this.auth.logout();
    this.router.navigate(['sign-in']);
  }

  getAuthData() {
    if (this.auth.getExpiration() == null) {
      return 'User logged: ' + this.auth.isLoggedIn() + ', expiration: not defined';
    } else {

      return 'User logged: ' + this.auth.isLoggedIn() + ', expiration: ' + this.auth.getExpiration().toLocaleString();
    }
  }

  ngOnInit() {
  }

  setLinks() {
    if (this.auth.isLoggedIn()) {
      if (this.auth.getRoles().includes('ROLE_USER')) {
        this.loggedIcon = ['people'];
        this.loggedLinks = ['presenze'];
        this.loggedTitle = ['Presenze'];
      }
      if (this.auth.getRoles().includes('ROLE_ADMIN')) {
        this.loggedIcon = ['people', 'schedule', 'notification_important'];
        this.loggedLinks = ['presenze', 'disponibilita', 'comunicazioni'];
        this.loggedTitle = ['Presenze', 'disponibilita', ''];
      }
      if (this.auth.getRoles().includes('ROLE_SYSTEM-ADMIN')) {
        this.loggedIcon = ['people', 'schedule', 'notification_important', 'verified_user'];
        this.loggedLinks = ['presenze', 'disponibilita', 'comunicazioni', 'turni'];
        this.loggedTitle = ['Presenze', 'disponibilita', '', ''];
      }
    }
  }

}
