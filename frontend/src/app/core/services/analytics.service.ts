import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { environment as env } from '@environments/environment';
import { AnalyticsQuery, SystemAnalytics } from '@shared/types/api.types';

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {
  private readonly http = inject(HttpClient);

  getSystemAnalytics(query: AnalyticsQuery) {
    const params = new HttpParams()
      .set('from', query.from)
      .set('to', query.to)
      .set('granularity', query.granularity)
      .set('topLimit', query.topLimit.toString());

    return this.http.get<SystemAnalytics>(`${env.apiUrl}/admin/analytics`, { params });
  }
}
