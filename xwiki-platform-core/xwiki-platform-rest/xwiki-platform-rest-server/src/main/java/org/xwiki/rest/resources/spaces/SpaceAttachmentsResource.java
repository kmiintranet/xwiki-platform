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
package org.xwiki.rest.resources.spaces;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.QueryException;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.resources.BaseAttachmentsResource;

/**
 * @version $Id$
 */
@Component("org.xwiki.rest.resources.spaces.SpaceAttachmentsResource")
@Path("/wikis/{wikiName}/spaces/{spaceName}/attachments")
public class SpaceAttachmentsResource extends BaseAttachmentsResource
{
    @Override
    @GET
    public Attachments getAttachments(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @QueryParam("name") @DefaultValue("") String name, @QueryParam("page") @DefaultValue("") String page,
        @QueryParam("author") @DefaultValue("") String author, @QueryParam("types") @DefaultValue("") String types,
        @QueryParam("start") @DefaultValue("0") Integer start, @QueryParam("number") @DefaultValue("25") Integer number)
        throws QueryException
    {
        return super.getAttachments(wikiName, name, page, spaceName, author, types, start, number);
    }
}