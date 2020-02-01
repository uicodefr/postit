import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse, HttpErrorResponse } from '@angular/common/http';

import { Observable, NEVER } from 'rxjs';
import { catchError, finalize, map } from 'rxjs/operators';
import { throwError } from 'rxjs';

import { GlobalInfoService } from './global-info.service';
import { AlertType } from '../../const/alert-type';
import { TranslateService } from './translate.service';

@Injectable({
  providedIn: 'root'
})
export class RestClientService {

  public constructor(
    private httpClient: HttpClient,
    private globalInfoService: GlobalInfoService,
    private translateService: TranslateService
  ) { }

  // GET, POST, PUT, DELETE in JSON

  public get<T>(url: string, paramData?: any): Observable<T> {
    this.globalInfoService.notifLoader(true);
    const obsResponse = this.httpClient.get(url,
      { observe: 'response', responseType: 'json', params: new HttpParams({ fromObject: paramData }) });
    return this.addMapperAndCatcher<T>(obsResponse, url);
  }

  public post<T>(url: string, paramData?: any): Observable<T> {
    this.globalInfoService.notifLoader(true);
    const obsResponse = this.httpClient.post(url, paramData,
      { observe: 'response', responseType: 'json' });
    return this.addMapperAndCatcher<T>(obsResponse, url);
  }

  public put<T>(url: string, paramData?: any): Observable<T> {
    this.globalInfoService.notifLoader(true);
    const obsResponse = this.httpClient.put(url, paramData,
      { observe: 'response', responseType: 'json' });
    return this.addMapperAndCatcher<T>(obsResponse, url);
  }

  public patch<T>(url: string, paramData?: any): Observable<T> {
    this.globalInfoService.notifLoader(true);
    const obsResponse = this.httpClient.patch(url, paramData,
      { observe: 'response', responseType: 'json' });
    return this.addMapperAndCatcher<T>(obsResponse, url);
  }

  public delete<T>(url: string): Observable<T> {
    this.globalInfoService.notifLoader(true);
    const obsResponse = this.httpClient.delete(url,
      { observe: 'response', responseType: 'json' });
    return this.addMapperAndCatcher<T>(obsResponse, url);
  }

  // Blob

  public getBlob(url: string, paramData?: any): Observable<{} | Blob> {
    this.globalInfoService.notifLoader(true);
    const obsResponse = this.httpClient.get(url,
      { observe: 'body', responseType: 'blob' }
    ).pipe(catchError(this.handleError(url))).pipe(finalize(() => {
      this.globalInfoService.notifLoader(false);
    }));
    return obsResponse;
  }

  // Internal Method

  private addMapperAndCatcher<T>(obsResponse: Observable<HttpResponse<Object>>, url: string): Observable<T> {
    return obsResponse.pipe(catchError(this.handleError(url)))
      .pipe(map((response) => response.body))
      .pipe(finalize(() => {
        this.globalInfoService.notifLoader(false);
      })) as Observable<T>;
  }

  private handleError(url: string) {
    const globalInfoService = this.globalInfoService;
    const translateService = this.translateService;

    return function (err) {
      let errMsg = url + ' - ';
      let errResult = null;

      if (err instanceof HttpErrorResponse) {

        if (err.status === 401) {
          globalInfoService.showAlert(AlertType.WARNING, translateService.get('Access Unauthorized. Please sign in.'));
          return NEVER;
        } else if (err.status === 403) {
          globalInfoService.showAlert(AlertType.DANGER, translateService.get('Access Forbidden !'));
          return NEVER;
        }

        errMsg = errMsg + err.status + ' ' + err.statusText;
        errResult = err.status;
      } else {
        errMsg = errMsg + err;
        errResult = errMsg;
      }

      globalInfoService.showAlert(AlertType.DANGER, 'Technical Error : ' + errMsg);
      return throwError(errResult);
    };
  }

}