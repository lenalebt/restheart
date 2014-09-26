/*
 * Copyright SoftInstigate srl. All Rights Reserved.
 *
 *
 * The copyright to the computer program(s) herein is the property of
 * SoftInstigate srl, Italy. The program(s) may be used and/or copied only
 * with the written permission of SoftInstigate srl or in accordance with the
 * terms and conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied. This copyright notice must not be removed.
 */
package com.softinstigate.restheart.handlers.collection;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;
import com.softinstigate.restheart.db.CollectionDAO;
import com.softinstigate.restheart.db.DocumentDAO;
import com.softinstigate.restheart.utils.ChannelReader;
import com.softinstigate.restheart.utils.HttpStatus;
import com.softinstigate.restheart.utils.RequestContext;
import com.softinstigate.restheart.utils.RequestHelper;
import com.softinstigate.restheart.utils.ResponseHelper;
import io.undertow.server.HttpServerExchange;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author uji
 */
public class PostCollectionHandler extends PutCollectionHandler
{
    private static final Logger logger = LoggerFactory.getLogger(PostCollectionHandler.class);
    
    /**
     * Creates a new instance of PostCollectionHandler
     */
    public PostCollectionHandler()
    {
    }

    @Override
    public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception
    {
        String _content = ChannelReader.read(exchange.getRequestChannel());

        DBObject content;

        try
        {
            content = (DBObject) JSON.parse(_content);
        }
        catch (JSONParseException ex)
        {
            ResponseHelper.endExchangeWithError(exchange, HttpStatus.SC_NOT_ACCEPTABLE, ex);
            return;
        }
        
        if (content == null)
            content = new BasicDBObject();
        
        // cannot POST an array
        if (content instanceof BasicDBList)
        {
            ResponseHelper.endExchange(exchange, HttpStatus.SC_NOT_ACCEPTABLE);
            return;
        }
        
        ObjectId etag = RequestHelper.getUpdateEtag(exchange);
        
        int SC = DocumentDAO.upsertDocumentPost(exchange, context.getDBName(), context.getCollectionName(), content, etag);

        ResponseHelper.endExchange(exchange, SC);
    }
    
    private Object getIdFromString(String id)
    {
        try
        {
            return new ObjectId(id);
        }
        catch(IllegalArgumentException ex)
        {
            // the id is not an object id
            return id;
        }
    }
    
    private BasicDBObject getIdQuery(Object id)
    {
        if (id instanceof ObjectId)
            return new BasicDBObject("_id", id);
        else
            return  new BasicDBObject("_id", getIdFromString((String) id));
    }
}
