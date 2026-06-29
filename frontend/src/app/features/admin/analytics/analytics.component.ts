import { NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  signal,
  ViewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AnalyticsService } from '@core/services/analytics.service';
import {
  AnalyticsGranularity,
  AnalyticsQuery,
  AnalyticsSeriesPoint,
  AnalyticsTopGroup,
  AnalyticsTopUser,
  SystemAnalytics,
} from '@shared/types/api.types';
import Chart from 'chart.js/auto';
import type { ChartConfiguration } from 'chart.js';
import { finalize } from 'rxjs';

interface MetricCard {
  label: string;
  value: number;
  tone: string;
}

@Component({
  templateUrl: './analytics.component.html',
  imports: [FormsModule, NgClass, RouterLink],
})
export class AnalyticsComponent {
  private readonly analyticsService = inject(AnalyticsService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('trendChart') private trendChartRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('topUsersChart') private topUsersChartRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('topGroupsChart') private topGroupsChartRef?: ElementRef<HTMLCanvasElement>;

  readonly granularities: AnalyticsGranularity[] = ['DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY'];

  query: AnalyticsQuery = {
    from: this.toDateInputValue(this.addDays(new Date(), -30)),
    to: this.toDateInputValue(new Date()),
    granularity: 'DAILY',
    topLimit: 10,
  };

  analytics = signal<SystemAnalytics | null>(null);
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  private trendChart?: Chart;
  private topUsersChart?: Chart;
  private topGroupsChart?: Chart;

  ngOnInit(): void {
    this.loadAnalytics();
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  loadAnalytics(): void {
    if (this.query.from > this.query.to) {
      this.isLoading.set(false);
      this.errorMessage.set('Start date must be before or equal to end date.');
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.analyticsService
      .getSystemAnalytics(this.query)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.isLoading.set(false)),
      )
      .subscribe({
        next: (analytics) => {
          this.analytics.set(analytics);
          this.isLoading.set(false);
          this.changeDetector.detectChanges();
          this.renderCharts(analytics);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.errorMessage.set(this.getErrorMessage(error));
        },
      });
  }

  updateGranularity(granularity: string): void {
    this.query = {
      ...this.query,
      granularity: granularity as AnalyticsGranularity,
    };
  }

  updateTopLimit(value: string | number): void {
    const parsedValue = Number(value);
    const topLimit = Number.isFinite(parsedValue) ? parsedValue : 10;

    this.query = {
      ...this.query,
      topLimit: Math.min(Math.max(Math.trunc(topLimit), 1), 100),
    };
  }

  metricCards(): MetricCard[] {
    const analytics = this.analytics();

    if (!analytics) {
      return [];
    }

    return [
      {
        label: 'Registered users',
        value: analytics.totalRegisteredUsers,
        tone: 'border-blue-200 bg-blue-50 text-blue-700',
      },
      {
        label: 'Active users',
        value: analytics.totalActiveUsers,
        tone: 'border-emerald-200 bg-emerald-50 text-emerald-700',
      },
      {
        label: 'Messages',
        value: analytics.totalExchangedMessages,
        tone: 'border-amber-200 bg-amber-50 text-amber-700',
      },
      {
        label: 'Created groups',
        value: analytics.totalCreatedGroups,
        tone: 'border-rose-200 bg-rose-50 text-rose-700',
      },
    ];
  }

  bucketLabel(point: AnalyticsSeriesPoint): string {
    if (point.bucketStart === point.bucketEnd) {
      return point.bucketStart;
    }

    return `${point.bucketStart} - ${point.bucketEnd}`;
  }

  formatNumber(value: number): string {
    return new Intl.NumberFormat().format(value);
  }

  userDisplayName(user: AnalyticsTopUser): string {
    return `${user.firstName} ${user.lastName}`;
  }

  groupDisplayName(group: AnalyticsTopGroup, index: number): string {
    return group.name?.trim() || `Group ${index + 1}`;
  }

  private renderCharts(analytics: SystemAnalytics): void {
    this.renderTrendChart(analytics.series);
    this.renderTopUsersChart(analytics.topUsers);
    this.renderTopGroupsChart(analytics.topGroups);
  }

  private renderTrendChart(series: AnalyticsSeriesPoint[]): void {
    this.trendChart?.destroy();
    this.trendChart = undefined;

    const canvas = this.trendChartRef?.nativeElement;
    if (!canvas) {
      return;
    }

    const labels = series.map((point) => this.bucketLabel(point));
    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Registered users',
            data: series.map((point) => point.registeredUsers),
            borderColor: '#2563eb',
            backgroundColor: 'rgba(37, 99, 235, 0.12)',
            tension: 0.25,
          },
          {
            label: 'Active users',
            data: series.map((point) => point.activeUsers),
            borderColor: '#059669',
            backgroundColor: 'rgba(5, 150, 105, 0.12)',
            tension: 0.25,
          },
          {
            label: 'Messages',
            data: series.map((point) => point.exchangedMessages),
            borderColor: '#d97706',
            backgroundColor: 'rgba(217, 119, 6, 0.12)',
            tension: 0.25,
          },
          {
            label: 'Created groups',
            data: series.map((point) => point.createdGroups),
            borderColor: '#e11d48',
            backgroundColor: 'rgba(225, 29, 72, 0.12)',
            tension: 0.25,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
          },
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              precision: 0,
            },
          },
        },
      },
    };

    this.trendChart = new Chart(canvas, config);
  }

  private renderTopUsersChart(users: AnalyticsTopUser[]): void {
    this.topUsersChart?.destroy();
    this.topUsersChart = undefined;

    const canvas = this.topUsersChartRef?.nativeElement;
    if (!canvas || users.length === 0) {
      return;
    }

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: users.map((user) => `@${user.username}`),
        datasets: [
          {
            label: 'Messages',
            data: users.map((user) => user.messageCount),
            backgroundColor: '#2563eb',
            borderRadius: 4,
          },
        ],
      },
      options: this.rankingChartOptions(),
    };

    this.topUsersChart = new Chart(canvas, config);
  }

  private renderTopGroupsChart(groups: AnalyticsTopGroup[]): void {
    this.topGroupsChart?.destroy();
    this.topGroupsChart = undefined;

    const canvas = this.topGroupsChartRef?.nativeElement;
    if (!canvas || groups.length === 0) {
      return;
    }

    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: groups.map((group, index) => this.groupDisplayName(group, index)),
        datasets: [
          {
            label: 'Messages',
            data: groups.map((group) => group.messageCount),
            backgroundColor: '#059669',
            borderRadius: 4,
          },
        ],
      },
      options: this.rankingChartOptions(),
    };

    this.topGroupsChart = new Chart(canvas, config);
  }

  private rankingChartOptions(): ChartConfiguration<'bar'>['options'] {
    return {
      indexAxis: 'y',
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false,
        },
      },
      scales: {
        x: {
          beginAtZero: true,
          ticks: {
            precision: 0,
          },
        },
      },
    };
  }

  private destroyCharts(): void {
    this.trendChart?.destroy();
    this.topUsersChart?.destroy();
    this.topGroupsChart?.destroy();
  }

  private getErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      return error.error?.detail ?? 'Unable to load analytics.';
    }

    return 'Unable to load analytics.';
  }

  private addDays(date: Date, days: number): Date {
    const nextDate = new Date(date);
    nextDate.setDate(nextDate.getDate() + days);
    return nextDate;
  }

  private toDateInputValue(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');

    return `${year}-${month}-${day}`;
  }
}
