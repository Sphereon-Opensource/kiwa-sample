package com.sphereon.kiwa.sample.app

import com.sphereon.di.session.SessionScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

/**
     * Component interface providing access to template provider factory from the DI graph.
     *
     * This interface is contributed to the [SessionScope], making the template
     * provider factory available as a session-scoped dependency. It allows the
     * ViewModel to create template providers with access to session-scoped services.
     */
    @ContributesTo(SessionScope::class)
    interface TemplateProviderComponent {
        /**
         * Factory for creating [TemplateProvider] instances.
         *
         * The factory is injected from the session-scoped dependency graph,
         * ensuring that created template providers have access to all necessary
         * session-scoped services and configuration.
         */
        val templateProviderFactory: TemplateProvider.Factory
    }
