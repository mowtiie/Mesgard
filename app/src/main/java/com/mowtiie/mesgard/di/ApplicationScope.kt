package com.mowtiie.mesgard.di

import javax.inject.Qualifier

/** A scope that lives as long as the process, for flows that several screens share. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ApplicationScope
