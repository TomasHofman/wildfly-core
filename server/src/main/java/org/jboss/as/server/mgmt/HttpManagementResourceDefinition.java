/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.mgmt;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.server.logging.ServerLogger.ROOT_LOGGER;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.as.server.operations.HttpManagementAddHandler;
import org.jboss.as.server.operations.HttpManagementRemoveHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the HTTP management interface resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HttpManagementResourceDefinition extends SimpleResourceDefinition {

    private static final PathElement RESOURCE_PATH = PathElement.pathElement(MANAGEMENT_INTERFACE, HTTP_INTERFACE);

    private static final OperationStepHandler VALIDATING_HANDLER = new HttpManagementValidatingHandler();

    public static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SECURITY_REALM, ModelType.STRING, true)
                .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
                .addAccessConstraint(new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SECURITY_REALM_REF))
                .setNullSignficant(true)
                .build();

    public static final SimpleAttributeDefinition INTERFACE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INTERFACE, ModelType.STRING, false)
                .setAllowExpression(true).setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                .setAlternatives(ModelDescriptionConstants.SOCKET_BINDING, ModelDescriptionConstants.SECURE_SOCKET_BINDING)
                .addAccessConstraint(new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SOCKET_CONFIG))
                .setDeprecated(ModelVersion.create(1,4))
                .build();

    public static final SimpleAttributeDefinition HTTP_PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PORT, ModelType.INT, true)
            .setAllowExpression(true).setValidator(new IntRangeValidator(0, 65535, true, true))
            .setAlternatives(ModelDescriptionConstants.SOCKET_BINDING, ModelDescriptionConstants.SECURE_SOCKET_BINDING)
            .setRequires(ModelDescriptionConstants.INTERFACE)
            .addAccessConstraint(new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SOCKET_CONFIG))
            .setDeprecated(ModelVersion.create(1,4))
            .build();

    public static final SimpleAttributeDefinition HTTPS_PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SECURE_PORT, ModelType.INT, true)
            .setAllowExpression(true).setValidator(new IntRangeValidator(0, 65535, true, true))
            .setAlternatives(ModelDescriptionConstants.SOCKET_BINDING, ModelDescriptionConstants.SECURE_SOCKET_BINDING)
            .setRequires(ModelDescriptionConstants.INTERFACE)
            .addAccessConstraint(new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SOCKET_CONFIG))
            .setDeprecated(ModelVersion.create(1,4))
            .build();

    public static final SimpleAttributeDefinition SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SOCKET_BINDING, ModelType.STRING, true)
            .setXmlName(Attribute.HTTP.getLocalName())
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setAlternatives(ModelDescriptionConstants.INTERFACE)
            .addAccessConstraint(new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SOCKET_CONFIG))
            .build();

    public static final SimpleAttributeDefinition SECURE_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SECURE_SOCKET_BINDING, ModelType.STRING, true)
            .setXmlName(Attribute.HTTPS.getLocalName())
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setAlternatives(ModelDescriptionConstants.INTERFACE)
            .addAccessConstraint(new SensitiveTargetAccessConstraintDefinition(SensitivityClassification.SOCKET_CONFIG))
            .build();

    public static final SimpleAttributeDefinition CONSOLE_ENABLED = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CONSOLE_ENABLED, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.CONSOLE_ENABLED.getLocalName())
            .setDefaultValue(new ModelNode(true))
            .build();

    public static final SimpleAttributeDefinition HTTP_UPGRADE_ENABLED = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.HTTP_UPGRADE_ENABLED, ModelType.BOOLEAN, true)
            .setXmlName(Attribute.HTTP_UPGRADE_ENABLED.getLocalName())
            .setDefaultValue(new ModelNode(false))
            .build();

    public static final SimpleAttributeDefinition SERVER_NAME = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SERVER_NAME, ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final SimpleAttributeDefinition SASL_PROTOCOL = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SASL_PROTOCOL, ModelType.STRING, true)
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
            .setDefaultValue(new ModelNode(ModelDescriptionConstants.REMOTE))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final StringListAttributeDefinition ALLOWED_ORIGINS = new StringListAttributeDefinition.Builder(ModelDescriptionConstants.ALLOWED_ORIGINS)
            .setAllowExpression(true)
            .setAllowNull(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .setAttributeMarshaller(AttributeMarshaller.STRING_LIST)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    public static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = new AttributeDefinition[] {INTERFACE, HTTP_PORT, HTTPS_PORT, SECURITY_REALM, SOCKET_BINDING,
                                                                                                 SECURE_SOCKET_BINDING, CONSOLE_ENABLED, HTTP_UPGRADE_ENABLED, SASL_PROTOCOL, SERVER_NAME, ALLOWED_ORIGINS};

    public static final HttpManagementResourceDefinition INSTANCE = new HttpManagementResourceDefinition();

    private final List<AccessConstraintDefinition> accessConstraints;

    private HttpManagementResourceDefinition() {
        super(RESOURCE_PATH,
                ServerDescriptions.getResourceDescriptionResolver("core.management.http-interface"),
                HttpManagementAddHandler.INSTANCE, HttpManagementRemoveHandler.INSTANCE,
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_NONE);
        this.accessConstraints = SensitiveTargetAccessConstraintDefinition.MANAGEMENT_INTERFACES.wrapAsList();
        setDeprecated(ModelVersion.create(1, 7));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeAttributeHandler = new WriteAttributeHandler(ATTRIBUTE_DEFINITIONS);
        for (AttributeDefinition attr : ATTRIBUTE_DEFINITIONS) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeAttributeHandler);
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    public static void addValidatingHandler(OperationContext operationContext, ModelNode fromOperation) {
        ModelNode operation = Util.createOperation("validate-http-interface", PathAddress.pathAddress(fromOperation.require(OP_ADDR)));

        operationContext.addStep(operation, VALIDATING_HANDLER, Stage.MODEL);
    }

    private static class WriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

        private WriteAttributeHandler(AttributeDefinition[] attributes) {
            super(attributes);
        }

        @Override
        protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName,
                ModelNode newValue, ModelNode oldValue, Resource model) throws OperationFailedException {
            super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
            addValidatingHandler(context, operation);
        }

    }

    private static class HttpManagementValidatingHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
            if (model.hasDefined(INTERFACE.getName())
                    && (model.hasDefined(SOCKET_BINDING.getName())
                    || model.hasDefined(SECURE_SOCKET_BINDING.getName())
            )) {
                throw ROOT_LOGGER.illegalCombinationOfHttpManagementInterfaceConfigurations(
                        INTERFACE.getName(),
                        SOCKET_BINDING.getName(),
                        SECURE_SOCKET_BINDING.getName());
            }

            if (model.hasDefined(SECURITY_REALM.getName()) == false
                    && (model.hasDefined(SECURE_SOCKET_BINDING.getName()) || model.hasDefined(HTTPS_PORT.getName()))) {
                throw ROOT_LOGGER.noSecurityRealmForSsl();
            }
        }

    }
}
