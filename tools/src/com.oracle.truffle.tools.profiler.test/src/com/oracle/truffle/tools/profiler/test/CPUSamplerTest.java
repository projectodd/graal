/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.tools.profiler.test;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.tools.profiler.CPUSampler;
import com.oracle.truffle.tools.profiler.ProfilerNode;
import org.graalvm.polyglot.Source;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;

public class CPUSamplerTest extends AbstractProfilerTest {

    private static CPUSampler sampler;

    final int executionCount = 10;

    @Before
    public void setupSampler() {
        sampler = CPUSampler.find(context.getEngine());
        Assert.assertNotNull(sampler);
        synchronized (sampler) {
            sampler.setGatherSelfHitTimes(true);
            sampler.setDelaySamplingUntilNonInternalLangInit(false);
        }
    }

    @Test
    public void testCollectingAndHasData() {

        sampler.setCollecting(true);

        Assert.assertEquals(0, sampler.getSampleCount());
        Assert.assertTrue(sampler.isCollecting());
        Assert.assertFalse(sampler.hasData());

        for (int i = 0; i < executionCount; i++) {
            execute(defaultSourceForSampling);
        }

        Assert.assertNotEquals(0, sampler.getSampleCount());
        Assert.assertTrue(sampler.isCollecting());
        Assert.assertTrue(sampler.hasData());

        sampler.setCollecting(false);

        Assert.assertFalse(sampler.isCollecting());
        Assert.assertTrue(sampler.hasData());

        sampler.clearData();
        Assert.assertFalse(sampler.isCollecting());
        Assert.assertEquals(0, sampler.getSampleCount());

        Assert.assertFalse(sampler.hasData());
    }

    Source defaultSourceForSampling = makeSource("ROOT(" +
                    "DEFINE(foo,ROOT(SLEEP(1)))," +
                    "DEFINE(bar,ROOT(BLOCK(STATEMENT,LOOP(10, CALL(foo)))))," +
                    "DEFINE(baz,ROOT(BLOCK(STATEMENT,LOOP(10, CALL(bar)))))," +
                    "CALL(baz),CALL(bar)" +
                    ")");

    @Test
    public void testCorrectRootStructure() {

        sampler.setFilter(NO_INTERNAL_ROOT_TAG_FILTER);
        sampler.setCollecting(true);
        for (int i = 0; i < executionCount; i++) {
            execute(defaultSourceForSampling);
        }

        Collection<ProfilerNode<CPUSampler.Payload>> children = sampler.getRootNodes();
        Assert.assertEquals(1, children.size());
        ProfilerNode<CPUSampler.Payload> program = children.iterator().next();
        Assert.assertEquals("", program.getRootName());
        checkTimeline(program.getPayload());

        children = program.getChildren();
        Assert.assertEquals(2, children.size());
        Iterator<ProfilerNode<CPUSampler.Payload>> iterator = children.iterator();
        ProfilerNode<CPUSampler.Payload> baz = iterator.next();
        if (!"baz".equals(baz.getRootName())) {
            baz = iterator.next();
        }
        Assert.assertEquals("baz", baz.getRootName());
        checkTimeline(baz.getPayload());

        children = baz.getChildren();
        Assert.assertEquals(1, children.size());
        ProfilerNode<CPUSampler.Payload> bar = children.iterator().next();
        Assert.assertEquals("bar", bar.getRootName());
        checkTimeline(bar.getPayload());

        children = bar.getChildren();
        Assert.assertEquals(1, children.size());
        ProfilerNode<CPUSampler.Payload> foo = children.iterator().next();
        Assert.assertEquals("foo", foo.getRootName());
        checkTimeline(foo.getPayload());

        children = foo.getChildren();
        Assert.assertTrue(children.size() == 0);
    }

    final Source defaultRecursiveSourceForSampling = makeSource("ROOT(" +
                    "DEFINE(foo,ROOT(BLOCK(RECURSIVE_CALL(foo, 10),SLEEP(1))))," +
                    "DEFINE(bar,ROOT(BLOCK(STATEMENT,LOOP(10, CALL(foo)))))," +
                    "CALL(bar)" +
                    ")");

    @Test
    @Ignore("non-deterministic failures on spark")
    public void testCorrectRootStructureRecursive() {

        sampler.setFilter(NO_INTERNAL_ROOT_TAG_FILTER);
        sampler.setCollecting(true);
        for (int i = 0; i < executionCount; i++) {
            execute(defaultRecursiveSourceForSampling);
        }

        Collection<ProfilerNode<CPUSampler.Payload>> children = sampler.getRootNodes();
        Assert.assertEquals(1, children.size());
        ProfilerNode<CPUSampler.Payload> program = children.iterator().next();
        Assert.assertEquals("", program.getRootName());
        checkTimeline(program.getPayload());

        children = program.getChildren();
        Assert.assertEquals(1, children.size());
        ProfilerNode<CPUSampler.Payload> bar = children.iterator().next();
        Assert.assertEquals("bar", bar.getRootName());
        checkTimeline(bar.getPayload());

        children = bar.getChildren();
        Assert.assertEquals(1, children.size());
        ProfilerNode<CPUSampler.Payload> foo = children.iterator().next();
        Assert.assertEquals("foo", foo.getRootName());
        checkTimeline(foo.getPayload());

        // RECURSIVE_CALL does recursions to depth 10
        for (int i = 0; i < 10; i++) {
            children = foo.getChildren();
            Assert.assertEquals(1, children.size());
            foo = children.iterator().next();
            Assert.assertEquals("foo", foo.getRootName());
            checkTimeline(bar.getPayload());
        }

        children = foo.getChildren();
        Assert.assertTrue(children.size() == 0);
    }

    @Test
    public void testShadowStackOverflows() {
        sampler.setFilter(NO_INTERNAL_ROOT_TAG_FILTER);
        sampler.setStackLimit(2);
        sampler.setCollecting(true);
        for (int i = 0; i < executionCount; i++) {
            execute(defaultSourceForSampling);
        }
        Assert.assertTrue(sampler.hasStackOverflowed());
    }

    private static void checkTimeline(CPUSampler.Payload payload) {
        Assert.assertEquals("Timeline length and self hit count to not match!", payload.getSelfHitCount(), payload.getSelfHitTimes().size());
    }

    @TruffleLanguage.Registration(id = RecreateShadowStackTestLanguage.ID, mimeType = RecreateShadowStackTestLanguage.MIME_TYPE, name = "RecreateShadowStackTestLanguage", version = "0.1")
    @ProvidedTags({StandardTags.StatementTag.class, StandardTags.RootTag.class})
    public static class RecreateShadowStackTestLanguage extends TruffleLanguage<Integer> {

        public static final String ID = "RecreateShadowStackTestLanguage";
        public static final String MIME_TYPE = "RecreateShadowStackTestLanguageMime";

        @Override
        protected Integer createContext(Env env) {
            return 0;
        }

        @Override
        protected boolean isObjectOfLanguage(Object object) {
            return false;
        }

        @Override
        protected boolean isThreadAccessAllowed(Thread thread, boolean singleThreaded) {
            return true;
        }

        @Override
        protected CallTarget parse(ParsingRequest request) throws Exception {
            com.oracle.truffle.api.source.Source source = request.getSource();
            SourceSection statement = null;
            SourceSection root = null;
            Statement startSamplerChild = null;

            // Case when we want to test statements and roots
            if (source.getCharacters().toString().equals("Statement Root")) {
                statement = source.createSection(0, 9);
                root = source.createSection(10, 4);
                startSamplerChild = new Statement(statement, new SleepNode());
            }

            // Case when we want to test roots only
            if (source.getCharacters().toString().equals("Root Root")) {
                statement = source.createUnavailableSection();
                root = source.createSection(0, 4);
                RootCallTarget sleepTarget = Truffle.getRuntime().createCallTarget(new SRootNode(this, new Root(root, new SleepNode())));
                startSamplerChild = new Statement(statement, new CallNode(Truffle.getRuntime().createDirectCallNode(sleepTarget)));
            }

            RootCallTarget innerTarget = Truffle.getRuntime().createCallTarget(
                            new SRootNode(this, new Root(root,
                                            new Wrapper(new Statement(statement, new Wrapper(new Statement(statement, new Wrapper(new StartSamplerNode(sampler, startSamplerChild)))))))));
            DirectCallNode directCallNode = Truffle.getRuntime().createDirectCallNode(innerTarget);
            return Truffle.getRuntime().createCallTarget(
                            new SRootNode(this, new Root(root, new Wrapper(new Statement(statement, new Wrapper(new Statement(statement, new Wrapper(new CallNode(directCallNode)))))))));

        }

        abstract static class SamplerTestNode extends Node {
            public abstract Object execute(VirtualFrame frame);
        }

        static class SleepNode extends SamplerTestNode {

            @Override
            public Object execute(VirtualFrame frame) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Assert.fail("Thread interrupted");
                }
                return 5;
            }
        }

        static class Wrapper extends SamplerTestNode {
            @Child SamplerTestNode node;

            Wrapper(SamplerTestNode node) {
                this.node = node;
                adoptChildren();
            }

            @Override
            public Object execute(VirtualFrame frame) {
                return node.execute(frame);
            }
        }

        @GenerateWrapper
        static class Statement extends SamplerTestNode implements InstrumentableNode {

            @Child SamplerTestNode node;
            SourceSection sourceSection;

            Statement(Statement other) {
                node = other.node;
                sourceSection = other.sourceSection;
                adoptChildren();
            }

            Statement(SourceSection sourceSection, SamplerTestNode node) {
                this.sourceSection = sourceSection;
                this.node = node;
            }

            @Override
            public SourceSection getSourceSection() {
                return sourceSection;
            }

            @Override
            public boolean isInstrumentable() {
                return true;
            }

            @Override
            public WrapperNode createWrapper(ProbeNode probe) {
                return new StatementWrapper(this, this, probe);
            }

            @Override
            public boolean hasTag(Class<? extends Tag> tag) {
                return tag == StandardTags.StatementTag.class;
            }

            @Override
            public Object execute(VirtualFrame frame) {
                return node.execute(frame);
            }
        }

        @GenerateWrapper
        static class Root extends SamplerTestNode implements InstrumentableNode {

            @Child SamplerTestNode node;
            SourceSection sourceSection;

            Root(Root other) {
                node = other.node;
                sourceSection = other.sourceSection;
                adoptChildren();
            }

            @Override
            public SourceSection getSourceSection() {
                return sourceSection;
            }

            Root(SourceSection sourceSection, SamplerTestNode node) {
                this.sourceSection = sourceSection;
                this.node = node;
            }

            @Override
            public boolean isInstrumentable() {
                return true;
            }

            @Override
            public WrapperNode createWrapper(ProbeNode probe) {
                return new RootWrapper(this, this, probe);
            }

            @Override
            public boolean hasTag(Class<? extends Tag> tag) {
                return tag == StandardTags.RootTag.class;
            }

            @Override
            public Object execute(VirtualFrame frame) {
                return node.execute(frame);
            }
        }

        static class SRootNode extends RootNode {

            private final RecreateShadowStackTestLanguage language;
            @Child SamplerTestNode child;

            SRootNode(RecreateShadowStackTestLanguage language, SamplerTestNode child) {
                super(language);
                this.language = language;
                this.child = child;
                adoptChildren();
            }

            protected SRootNode(SRootNode other) {
                super(other.language);
                language = other.language;
                child = other.child;
                adoptChildren();
            }

            @Override
            public Object execute(VirtualFrame frame) {
                return child.execute(frame);
            }
        }

        static class CallNode extends SamplerTestNode {
            @Child DirectCallNode callNode;

            CallNode(DirectCallNode callNode) {
                this.callNode = callNode;
                adoptChildren();
            }

            @Override
            public Object execute(VirtualFrame frame) {
                return callNode.call(new Object[]{});
            }
        }

        static class StartSamplerNode extends SamplerTestNode {
            CPUSampler sampler;
            @Child SamplerTestNode child;

            StartSamplerNode(CPUSampler sampler, SamplerTestNode child) {
                this.sampler = sampler;
                this.child = child;
                adoptChildren();
            }

            @Override
            public Object execute(VirtualFrame frame) {
                Assert.assertTrue("Found roots before enabling sampler", sampler.getRootNodes().isEmpty());
                sampler.setCollecting(true);
                return child.execute(frame);
            }
        }
    }

    @Test
    public void testCorrectInitShadowStackStatements() {
        sampler.setMode(CPUSampler.Mode.STATEMENTS);
        sampler.setFilter(NO_INTERNAL_STATEMENT_TAG_FILTER);
        Source test = Source.newBuilder(RecreateShadowStackTestLanguage.ID, "Statement Root", "test").buildLiteral();
        context.eval(test);
        Collection<ProfilerNode<CPUSampler.Payload>> rootNodes = sampler.getRootNodes();

        ProfilerNode<CPUSampler.Payload> current = rootNodes.iterator().next();
        Assert.assertEquals("Stack not correct", "Root", current.getSourceSection().getCharacters().toString());
        Assert.assertTrue("Stack not deep enough", current.getChildren().iterator().hasNext());

        current = current.getChildren().iterator().next();
        Assert.assertEquals("Stack not correct", "Statement", current.getSourceSection().getCharacters().toString());
        Assert.assertTrue("Stack not deep enough", current.getChildren().iterator().hasNext());

        current = current.getChildren().iterator().next();
        Assert.assertEquals("Stack not correct", "Statement", current.getSourceSection().getCharacters().toString());
        Assert.assertTrue("Stack not deep enough", current.getChildren().iterator().hasNext());

        current = current.getChildren().iterator().next();
        Assert.assertEquals("Stack not correct", "Root", current.getSourceSection().getCharacters().toString());
        Assert.assertTrue("Stack not deep enough", current.getChildren().iterator().hasNext());

        current = current.getChildren().iterator().next();
        Assert.assertEquals("Stack not correct", "Statement", current.getSourceSection().getCharacters().toString());
        Assert.assertTrue("Stack not deep enough", current.getChildren().iterator().hasNext());

        current = current.getChildren().iterator().next();
        Assert.assertEquals("Stack not correct", "Statement", current.getSourceSection().getCharacters().toString());
        Assert.assertTrue("Stack not deep enough", current.getChildren().iterator().hasNext());

        current = current.getChildren().iterator().next();
        Assert.assertEquals("Stack not correct", "Statement", current.getSourceSection().getCharacters().toString());
        Assert.assertFalse("Stack too deep", current.getChildren().iterator().hasNext());
    }

    @Test
    public void testCorrectInitShadowStackRoots() {
        sampler.setMode(CPUSampler.Mode.ROOTS);
        sampler.setFilter(NO_INTERNAL_ROOT_TAG_FILTER);
        Source test = Source.newBuilder(RecreateShadowStackTestLanguage.ID, "Root Root", "test").buildLiteral();
        context.eval(test);
        Collection<ProfilerNode<CPUSampler.Payload>> rootNodes = sampler.getRootNodes();

        ProfilerNode<CPUSampler.Payload> current = rootNodes.iterator().next();
        Assert.assertEquals("Stack not correct", "Root", current.getSourceSection().getCharacters().toString());
        Assert.assertTrue("Stack not deep enough", current.getChildren().iterator().hasNext());

        current = current.getChildren().iterator().next();
        Assert.assertEquals("Stack not correct", "Root", current.getSourceSection().getCharacters().toString());
        Assert.assertTrue("Stack not deep enough", current.getChildren().iterator().hasNext());

        current = current.getChildren().iterator().next();
        Assert.assertEquals("Stack not correct", "Root", current.getSourceSection().getCharacters().toString());
        Assert.assertFalse("Stack too deep", current.getChildren().iterator().hasNext());
    }
}
