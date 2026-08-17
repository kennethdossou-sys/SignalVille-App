import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ReportPage } from '../../shared/models/api.models';

@Injectable({ providedIn: 'root' })
export class Reports {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/reports`;

  list(page = 0, size = 20): Observable<ReportPage> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);

    return this.http.get<ReportPage>(this.baseUrl, { params });
  }
}