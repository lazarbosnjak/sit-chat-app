import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { environment as env } from '@environments/environment';
import {
  DataTableComponent,
  TableColumn,
} from '@shared/components/data-table/data-table.component';
import { Page } from '@shared/types/api.types';

interface RegistrationRequest {
  requestId: string;
  username: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  email: string;
  pfpUrl: string;
  status: 'APPROVED' | 'IN_PROCESS' | 'REJECTED';
}

@Component({
  templateUrl: './registration-request.component.html',
  imports: [DataTableComponent],
})
export class RegistrationRequestsComponent {
  private http = inject(HttpClient);

  columns: TableColumn<RegistrationRequest>[] = [
    { key: 'requestId', label: 'Request ID' },
    { key: 'email', label: 'E-mail' },
    { key: 'username', label: 'Username' },
    { key: 'firstName', label: 'First name' },
    { key: 'lastName', label: 'Last name' },
    { key: 'phoneNumber', label: 'Phone number' },
    { key: 'pfpUrl', label: 'Pfp url' },
    { key: 'status', label: 'Status' },
  ];

  regReqs = signal<RegistrationRequest[]>([]);

  ngOnInit() {
    this.http
      .get<Page<RegistrationRequest>>(`${env.apiUrl}/admin/registration-requests`)
      .subscribe({
        next: (res) => {
          this.regReqs.set(res.content);
        },
        error: (err) => {
          console.log(err);
        },
      });
  }

  approve(regReq: RegistrationRequest) {
    const url = `${env.apiUrl}/admin/registration-requests/${regReq.requestId}/approve`;
    this.http.patch<void>(url, null).subscribe({
      next: () => {
        this.regReqs.update((reqs) =>
          reqs.map((req) => {
            if (req.requestId === regReq.requestId) {
              req.status = 'APPROVED';
              return req;
            }
            return req;
          }),
        );
      },
      error: (err) => {
        console.log(err);
      },
    });
  }

  reject(regReq: RegistrationRequest) {
    const url = `${env.apiUrl}/admin/registration-requests/${regReq.requestId}/reject`;
    this.http.patch<void>(url, null).subscribe({
      next: () => {
        this.regReqs.update((reqs) =>
          reqs.map((req) => {
            if (req.requestId === regReq.requestId) {
              req.status = 'REJECTED';
              return req;
            }
            return req;
          }),
        );
      },
      error: (err) => {
        console.log(err);
      },
    });
  }
}
