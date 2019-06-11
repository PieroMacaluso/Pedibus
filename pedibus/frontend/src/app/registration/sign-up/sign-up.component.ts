import {Component, OnInit} from '@angular/core';
import {SignUpModel} from '../models';
import {AuthService} from '../auth.service';
import {HttpErrorResponse} from '@angular/common/http';

@Component({
  selector: 'app-sign-up',
  templateUrl: './sign-up.component.html',
  styleUrls: ['./sign-up.component.scss']
})
export class SignUpComponent implements OnInit {

  model: SignUpModel;
  serverErrors: string;
  isPresent: boolean;

  constructor(private auth: AuthService) {
    this.model = {email: '', password: '', passMatch: ''};
  }

  ngOnInit() {
  }

  checkPresence() {
    return this.isPresent;
  }

  submit(event) {
    if (event.isTrusted) {
      let formValid = true;
      for (let count = 0; count < 4; count++) {
        if (!event.target[count].validity.valid) {
          formValid = false;
        }
      }
      if (this.isPresent) {
        formValid = false;
      }
      if (formValid) {
        this.auth.signUp(this.model).subscribe((value => {
          console.log(value);
        }), error1 => {
          this.serverErrors = (error1 as HttpErrorResponse).error.errorMessage;
        });
      }
    }
  }

  checkDuplicate() {
    if (this.validateEmail(this.model.email)) {
      this.auth.checkDuplicate(this.model.email).subscribe((value => {
        typeof value === 'boolean' ? this.isPresent = value : this.isPresent = false;
        console.log(this.isPresent);
      }), error1 => {
        this.serverErrors = (error1 as HttpErrorResponse).error.errorMessage;
      });
    }
  }

  validateEmail(email) {
    const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(String(email).toLowerCase());
  }
}