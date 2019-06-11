import {Injectable} from '@angular/core';
import {CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, Router} from '@angular/router';
import {Observable} from 'rxjs';
import {AuthService} from './registration/auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private auth: AuthService, private router: Router) {
  }

  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    // Check that the user is logged in...
    if (this.auth.isLoggedIn()) {
      if (state.url === '/sign-up') {
        this.router.navigate(['presenze']);
        return false;
      }
      if (state.url === '/sign-in') {
        this.router.navigate(['presenze']);
        return false;
      }
    } else {
      if (state.url !== '/sign-up' && state.url !== '/sign-in') {
        this.router.navigate(['sign-in']);
        return false;
      }
    }
    return true;

  }

}