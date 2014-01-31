/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.pull.shared.evaluators;

import org.eclipse.egit.github.core.PullRequest;
import org.jboss.pull.shared.PullHelper;
import org.jboss.pull.shared.Util;
import org.jboss.pull.shared.spi.PullEvaluator;

import java.util.List;
import java.util.Properties;

/**
 * An abstract base evaluator which holds the target github branch.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public abstract class BasePullEvaluator implements PullEvaluator {
    protected PullHelper helper;
    protected Properties configuration;

    protected String githubBranch;

    @Override
    public void init(final PullHelper helper, final Properties configuration) {
        this.helper = helper;
        this.configuration = configuration;
        this.githubBranch = Util.require(configuration, getGithubBranchPropertyName());
    }

    /**
     * A property name of a github branch this evaluator should be dedicated to.
     * @return property name of the target github branch
     */
    protected abstract String getGithubBranchPropertyName();

    @Override
    public String getTargetBranch() {
        return githubBranch;
    }


    protected Result isMergeableByUpstream(final PullRequest pull) {
        final Result mergeable = new Result(true);

        try {
            final List<PullRequest> upstreamPulls = helper.getUpstreamPullRequest(pull);
            if (upstreamPulls.isEmpty()) {
                mergeable.setMergeable(false);
                mergeable.addDescription("Missing any upstream pull request");
                return mergeable;
            }

            for (PullRequest pullRequest : upstreamPulls) {
                if (! helper.isMerged(pullRequest)) {
                    mergeable.setMergeable(false);
                    mergeable.addDescription("Upstream pull request #" + pullRequest.getNumber() + " has not been merged yet");
                }
            }
        } catch (Exception ignore) {
            System.err.printf("Cannot get an upstream pull request of the pull request %d: %s.\n", pull.getNumber(), ignore);
            ignore.printStackTrace(System.err);

            mergeable.setMergeable(false);
            mergeable.addDescription("Cannot get an upstream pull request of the pull request " + pull.getNumber() + ": " + ignore.getMessage());
        }

        if (mergeable.isMergeable()) {
            mergeable.addDescription("Upstream pull request is OK");
        }

        return mergeable;
    }

}