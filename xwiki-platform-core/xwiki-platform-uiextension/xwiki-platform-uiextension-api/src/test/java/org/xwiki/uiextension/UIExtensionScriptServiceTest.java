/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
package org.xwiki.uiextension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;
import org.xwiki.uiextension.internal.WikiUIExtension;
import org.xwiki.uiextension.internal.WikiUIExtensionConstants;
import org.xwiki.uiextension.internal.scripting.UIExtensionScriptService;
import org.xwiki.velocity.VelocityManager;

import junit.framework.Assert;

@MockingRequirement(value = UIExtensionScriptService.class)
public class UIExtensionScriptServiceTest extends AbstractMockingComponentTestCase
    implements WikiUIExtensionConstants
{
    private static final DocumentReference CLASS_REF = new DocumentReference("xwiki", "XWiki", "UIExtensionClass");

    private static final DocumentReference DOC_REF = new DocumentReference("xwiki", "XWiki", "MyUIExtension");

    private ObjectReference OBJ1_REF = new ObjectReference(CLASS_REF + "[1]", DOC_REF);

    private ObjectReference OBJ2_REF = new ObjectReference(CLASS_REF + "[2]", DOC_REF);

    private ObjectReference OBJ3_REF = new ObjectReference(CLASS_REF + "[3]", DOC_REF);

    private ObjectReference OBJ4_REF = new ObjectReference(CLASS_REF + "[4]", DOC_REF);

    private ComponentManager contextComponentManager;

    private UIExtensionScriptService service;

    private Map<String, UIExtension> uiExtensions = new HashMap<String, UIExtension>();

    private BlockRenderer blockRenderer;

    private Transformation transformation;

    private Execution execution;

    XDOM xdom = new XDOM(new ArrayList<Block>());

    @Before
    public void configure() throws Exception
    {
        execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext context = new ExecutionContext();

        contextComponentManager =
            getComponentManager().registerMockComponent(getMockery(), ComponentManager.class, "context", "context");
        final Provider<ComponentManager> componentManagerProvider =
            getComponentManager().getInstance(
                new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        blockRenderer =
            getComponentManager().registerMockComponent(getMockery(), BlockRenderer.class,
                Syntax.XHTML_1_0.toIdString());
        getComponentManager().registerMockComponent(getMockery(), VelocityManager.class);
        transformation = getComponentManager().registerMockComponent(getMockery(), Transformation.class, "macro");
        this.service = getComponentManager().getInstance(ScriptService.class, "uix");
        uiExtensions.put("1id3", new WikiUIExtension(OBJ1_REF, "1id3", "epId1", xdom, Syntax.XWIKI_2_0,
            new HashMap<String, String>(), getComponentManager()));
        uiExtensions.put("1id1", new WikiUIExtension(OBJ2_REF, "1id1", "epId1", xdom, Syntax.XWIKI_2_0,
            new HashMap<String, String>(), getComponentManager()));
        uiExtensions.put("1id2", new WikiUIExtension(OBJ3_REF, "1id2", "epId1", xdom, Syntax.XWIKI_2_0,
            new HashMap<String, String>(), getComponentManager()));
        uiExtensions.put("2id1", new WikiUIExtension(OBJ4_REF, "2id1", "epId2", xdom, Syntax.XWIKI_2_0,
            new HashMap<String, String>(), getComponentManager()));

        getMockery().checking(new Expectations()
        {
            {
                allowing(execution).getContext();
                will(returnValue(context));
                allowing(componentManagerProvider).get();
                will(returnValue(contextComponentManager));
            }
        });
    }

    @Test
    public void getExtensionsWhenExtensionsExistForTheExtensionPoint() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(contextComponentManager).getInstanceMap(UIExtension.class);
                will(returnValue(uiExtensions));
            }
        });

        Collection<UIExtension> extensions = this.service.getExtensions("epId1");
        Assert.assertEquals(3, extensions.size());
    }

    @Test
    public void getExtensionsWhenNoExtensionsForTheExtensionPoint() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(contextComponentManager).getInstanceMap(UIExtension.class);
                will(returnValue(uiExtensions));
            }
        });

        Collection<UIExtension> extensions = this.service.getExtensions("epId3");
        Assert.assertEquals(0, extensions.size());
    }

    @Test
    public void verifyExtensionsAreSortedAlphabeticallyById() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(contextComponentManager).getInstanceMap(UIExtension.class);
                will(returnValue(uiExtensions));
            }
        });

        List<UIExtension> extensions = new ArrayList<UIExtension>();
        extensions.addAll(this.service.getExtensions("epId1"));

        Assert.assertEquals("1id1", extensions.get(0).getName());
        Assert.assertEquals("1id2", extensions.get(1).getName());
        Assert.assertEquals("1id3", extensions.get(2).getName());
    }

    @Test
    public void getExtensionsWhenLookupFails() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(contextComponentManager).getInstanceMap(UIExtension.class);
                will(throwException(new ComponentLookupException("")));
                // Make sure it prints an error in the log
                oneOf(getMockLogger()).error(with(any(String.class)), with(any(ComponentLookupException.class)));
            }
        });

        Collection<UIExtension> extensions = this.service.getExtensions("doesn'tMatter");
        Assert.assertEquals(0, extensions.size());
    }



    @Test
    public void render() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(contextComponentManager).getInstanceMap(UIExtension.class);
                will(returnValue(uiExtensions));
                allowing(transformation).transform(with(any(XDOM.class)), with(any(TransformationContext.class)));
                allowing(blockRenderer).render(with(any(Block.class)), with(any(WikiPrinter.class)));
            }
        });

        String result = this.service.render("epId1");
        Assert.assertEquals("", result);
    }
}