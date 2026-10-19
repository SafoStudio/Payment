import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface GraphqlResponse<T> {
  data?: T;
  errors?: Array<{ message: string; extensions?: Record<string, unknown> }>;
}

@Service()
export class GraphqlService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = '/graphql';

  query<T>(query: string, variables?: Record<string, unknown>): Observable<GraphqlResponse<T>> {
    return this.http.post<GraphqlResponse<T>>(this.endpoint, { query, variables });
  }
}
