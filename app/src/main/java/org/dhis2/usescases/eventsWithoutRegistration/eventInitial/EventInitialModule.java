package org.dhis2.usescases.eventsWithoutRegistration.eventInitial;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.sqlbrite2.BriteDatabase;

import org.dhis2.data.dagger.PerActivity;
import org.dhis2.data.forms.EventRepository;
import org.dhis2.data.forms.FormRepository;
import org.dhis2.data.forms.RulesRepository;
import org.dhis2.data.schedulers.SchedulerProvider;
import org.dhis2.usescases.eventsWithoutRegistration.eventSummary.EventSummaryRepository;
import org.dhis2.usescases.eventsWithoutRegistration.eventSummary.EventSummaryRepositoryImpl;
import org.dhis2.utils.analytics.AnalyticsHelper;
import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.rules.RuleExpressionEvaluator;

import dagger.Module;
import dagger.Provides;

/**
 * QUADRAM. Created by Cristian on 13/02/2018.
 */
@PerActivity
@Module
public class EventInitialModule {

    private final EventInitialContract.View view;
    @Nullable
    private String eventUid;

    public EventInitialModule(EventInitialContract.View view, @Nullable String eventUid) {
        this.view = view;
        this.eventUid = eventUid;
    }

    @Provides
    @PerActivity
    EventInitialContract.View provideView(EventInitialContract.View activity) {
        return activity;
    }

    @Provides
    @PerActivity
    EventInitialContract.Presenter providesPresenter(@NonNull EventSummaryRepository eventSummaryRepository,
                                                     @NonNull EventInitialRepository eventInitialRepository,
                                                     @NonNull SchedulerProvider schedulerProvider,
                                                     AnalyticsHelper analyticsHelper) {
        return new EventInitialPresenter(view, eventSummaryRepository, eventInitialRepository, schedulerProvider, analyticsHelper);
    }


    @Provides
    @PerActivity
    EventSummaryRepository eventSummaryRepository(@NonNull Context context,
                                                  @NonNull BriteDatabase briteDatabase,
                                                  @NonNull FormRepository formRepository, D2 d2) {
        return new EventSummaryRepositoryImpl(context, briteDatabase, formRepository, eventUid, d2);
    }

    @Provides
    FormRepository formRepository(@NonNull RuleExpressionEvaluator evaluator,
                                  @NonNull RulesRepository rulesRepository,
                                  @NonNull D2 d2) {
        return new EventRepository(evaluator, rulesRepository, eventUid, d2);
    }

    @Provides
    RulesRepository rulesRepository(@NonNull D2 d2) {
        return new RulesRepository(d2);
    }

    @Provides
    @PerActivity
    EventInitialRepository eventDetailRepository(D2 d2) {
        return new EventInitialRepositoryImpl(eventUid, d2);
    }
}
