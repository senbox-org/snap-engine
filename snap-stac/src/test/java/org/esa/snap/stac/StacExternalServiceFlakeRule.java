/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.stac;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * JUnit 4 rule that converts external-service failures (read timeouts and
 * HTTP 5xx / 408 / 429 responses) into JUnit assumptions, so the test is
 * SKIPPED rather than FAILED when the live STAC service is degraded.
 *
 * Walks the full exception chain so wrapped causes (e.g. an IOException
 * produced by {@link StacItem} re-throwing a network failure) are detected.
 */
final class StacExternalServiceFlakeRule implements TestRule {

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } catch (Throwable t) {
                    Throwable c = t;
                    while (c != null) {
                        if (c instanceof SocketTimeoutException) {
                            throw new AssumptionViolatedException(
                                    "Skipped: external STAC service read timeout", c);
                        }
                        if (c instanceof IOException) {
                            String msg = c.getMessage();
                            if (msg != null
                                    && (msg.contains("HTTP 502")
                                    || msg.contains("HTTP 503")
                                    || msg.contains("HTTP 504")
                                    || msg.contains("HTTP 408")
                                    || msg.contains("HTTP 429"))) {
                                throw new AssumptionViolatedException(
                                        "Skipped: external STAC service transient error - " + msg, c);
                            }
                        }
                        c = c.getCause();
                    }
                    throw t;
                }
            }
        };
    }
}
