/*
 * Copyright (c) 2010-2018. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.messaging.interceptors;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.axonframework.messaging.GenericMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.Log4jLoggerAdapter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

/**
 * Test cases to verify the logger of a {@link LoggingInterceptor} is called when expected.
 *
 * @author Allard Buijze
 * @author Nakul Mishra
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
class LoggingInterceptorTest {

    private LoggingInterceptor<Message<?>> testSubject;
    private org.apache.log4j.Logger mockLogger;
    private InterceptorChain interceptorChain;
    private UnitOfWork<Message<?>> unitOfWork;

    @BeforeEach
    void setUp() throws Exception {
        testSubject = new LoggingInterceptor<>();

        Log4jLoggerAdapter logger = (Log4jLoggerAdapter) LoggerFactory.getLogger(LoggingInterceptor.class);
        Field loggerField = logger.getClass().getDeclaredField("logger");
        ReflectionUtils.makeAccessible(loggerField);

        mockLogger = mock(Logger.class);
        loggerField.set(logger, mockLogger);

        interceptorChain = mock(InterceptorChain.class);
        unitOfWork = new DefaultUnitOfWork<>(new GenericMessage<Object>(new StubMessage()));
    }

    @Test
    void testConstructorWithCustomLogger() throws Exception {
        testSubject = new LoggingInterceptor<>("my.custom.logger");
        Field field = testSubject.getClass().getDeclaredField("logger");
        field.setAccessible(true);
        Log4jLoggerAdapter logger = (Log4jLoggerAdapter) field.get(testSubject);
        assertEquals("my.custom.logger", logger.getName());
    }

    @Test
    void testHandlerInterceptorWithIncomingLoggingNullReturnValue() throws Exception {
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(interceptorChain.proceed()).thenReturn(null);

        testSubject.handle(unitOfWork, interceptorChain);

        verify(mockLogger, atLeast(1)).isInfoEnabled();
        verify(mockLogger, times(2)).log(any(String.class), any(Priority.class), contains("[StubMessage]"),
                                         any());
        verify(mockLogger).log(any(String.class), any(Priority.class), and(contains("[StubMessage]"),
                                                                           contains("[null]")), any());
        verifyNoMoreInteractions(mockLogger);
    }

    @Test
    void testHandlerInterceptorWithSuccessfulExecutionVoidReturnValue() throws Exception {
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(interceptorChain.proceed()).thenReturn(null);

        testSubject.handle(unitOfWork, interceptorChain);

        verify(mockLogger, atLeast(1)).isInfoEnabled();
        verify(mockLogger, times(2)).log(any(String.class), any(Priority.class), contains("[StubMessage]"),
                                         any());
        verify(mockLogger).log(any(String.class), any(Priority.class), and(contains("[StubMessage]"),
                                                                           contains("[null]")), any());
        verifyNoMoreInteractions(mockLogger);
    }

    @Test
    void testHandlerInterceptorWithSuccessfulExecutionCustomReturnValue() throws Exception {
        when(interceptorChain.proceed()).thenReturn(new StubResponse());
        when(mockLogger.isInfoEnabled()).thenReturn(true);

        testSubject.handle(unitOfWork, interceptorChain);

        verify(mockLogger, atLeast(1)).isInfoEnabled();
        verify(mockLogger).log(anyString(), eq(Level.INFO),
                               and(contains("[StubMessage]"), contains("[StubResponse]")),
                               any());
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    void testHandlerInterceptorWithFailedExecution() throws Exception {
        RuntimeException exception = new RuntimeException();
        when(interceptorChain.proceed()).thenThrow(exception);
        when(mockLogger.isInfoEnabled()).thenReturn(true);

        try {
            testSubject.handle(unitOfWork, interceptorChain);
            fail("Expected exception to be propagated");
        } catch (RuntimeException e) {
            // expected
        }

        verify(mockLogger).log(any(String.class), eq(Level.WARN), and(contains("[StubMessage]"),
                                                                      contains("failed")), eq(exception));
    }

    @Test
    void testDispatchInterceptorLogging() {
        when(mockLogger.isInfoEnabled()).thenReturn(true);

        testSubject.handle(new GenericMessage<Object>(new StubMessage()));

        verify(mockLogger, atLeast(1)).isInfoEnabled();
        verify(mockLogger).log(
                any(String.class),
                eq(Level.INFO),
                contains("[StubMessage]"),
                any()
        );

        verifyNoMoreInteractions(mockLogger);
    }

    private static class StubMessage {

    }

    private static class StubResponse {

    }
}
