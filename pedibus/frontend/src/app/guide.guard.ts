import {Injectable} from '@angular/core';
import {CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, Router} from '@angular/router';
import {Observable} from 'rxjs';
import {AuthService} from './registration/auth.service';

@Injectable({
  providedIn: 'root'
})
export class GuideGuard implements CanActivate {

  constructor(private auth: AuthService, private router: Router) {
  }

  canActivate(route: ActivatedRouteSnapshot,
              state: RouterStateSnapshot): boolean | UrlTree | Observable<boolean | UrlTree> | Promise<boolean | UrlTree> {

    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['sign-in']);
      return false;
    }

    if (!this.auth.isGuide() && !this.auth.isAdmin()) {
      this.router.navigate(['genitore']);
      return false;
    }

    return true;
  }

}
