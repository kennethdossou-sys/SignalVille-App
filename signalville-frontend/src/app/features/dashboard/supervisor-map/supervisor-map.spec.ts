import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SupervisorMap } from './supervisor-map';

describe('SupervisorMap', () => {
  let component: SupervisorMap;
  let fixture: ComponentFixture<SupervisorMap>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SupervisorMap],
    }).compileComponents();

    fixture = TestBed.createComponent(SupervisorMap);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
