import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { StopMapComponent } from './stop-map.component';

describe('StopMapComponent', () => {
  let component: StopMapComponent;
  let fixture: ComponentFixture<StopMapComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ StopMapComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(StopMapComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
