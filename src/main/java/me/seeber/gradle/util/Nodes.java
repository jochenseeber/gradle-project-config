/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2016-2017, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package me.seeber.gradle.util;

import static java.lang.String.format;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility functions for {@link Node}
 */
public abstract class Nodes {

    /**
     * Encapsulates a {@link NodeList} as {@link List}
     */
    protected static class ListNodeList extends AbstractList<Node> {

        /**
         * Node list to wrap
         */
        protected final NodeList nodes;

        /**
         * Create a new list
         *
         * @param nodes Node list to wrap
         */
        public ListNodeList(NodeList nodes) {
            this.nodes = nodes;
        }

        /**
         * @see java.util.AbstractList#get(int)
         */
        @Override
        public Node get(int index) {
            if (index < 0 || index >= this.nodes.getLength()) {
                throw new IndexOutOfBoundsException(String.format("Illegal node index %d", index));
            }

            @NonNull Node node = Validate.notNull(this.nodes.item(index));
            return node;
        }

        /**
         * @see java.util.AbstractCollection#size()
         */
        @Override
        public int size() {
            return this.nodes.getLength();
        }

    }

    /**
     * Get the child elements of a node
     *
     * @param nodes Node to get the children of
     * @return Child elements
     */
    public static List<@NonNull Element> elements(NodeList nodes) {
        List<@NonNull Element> elements = new ArrayList<>(nodes.getLength());

        for (int i = 0; i < nodes.getLength(); ++i) {
            Node child = nodes.item(i);

            if (child instanceof Element) {
                elements.add((Element) child);
            }
        }

        return elements;
    }

    /**
     * Return a node list as {@link List}
     *
     * @param nodes Node list to wrap
     * @return Nodes
     */
    public static List<Node> asList(NodeList nodes) {
        return new ListNodeList(nodes);
    }

    /**
     * Get an attribute value from a node
     *
     * @param node Node to get attribute from
     * @param name Attribute name
     * @return Attribute value
     * @throws NoSuchElementException if the attribute does not exist
     */
    public static String attributeValue(Node node, String name) throws NoSuchElementException {
        @NonNull NamedNodeMap attributes = Validate.notNull(node.getAttributes());
        Node attribute = attributes.getNamedItem(name);

        if (attribute == null) {
            throw new NoSuchElementException(format("Element '%s' has no attribute '%s'", node.getNodeName(), name));
        }

        String value = attribute.getNodeValue();

        if (value == null) {
            throw new NoSuchElementException(
                    format("Attribute '%s' of element '%s' has no value", name, node.getNodeName()));
        }

        return value;
    }

    /**
     * Set the attribute value of a node
     *
     * @param node Node to set attribute
     * @param attribute Name of attribute to set
     * @param value Attribute value
     */
    public static void setAttribute(Node node, String attribute, String value) {
        @NonNull Document document = Validate.notNull(node.getOwnerDocument());
        @NonNull NamedNodeMap attributes = Validate.notNull(node.getAttributes());
        Node attributeNode = attributes.getNamedItem(attribute);

        if (attributeNode == null) {
            attributeNode = document.createAttribute(attribute);
            attributes.setNamedItem(attributeNode);
        }

        attributeNode.setNodeValue(value);
    }

    /**
     * Get or create a child element
     *
     * @param parent Parent element
     * @param childName Child to get or create
     * @return Child element
     */
    public static Element child(Element parent, String childName) {
        NodeList children = parent.getElementsByTagName(childName);
        @NonNull Element child;

        if (children.getLength() == 0) {
            @NonNull Document document = Validate.notNull(parent.getOwnerDocument());
            child = document.createElement(childName);
            parent.appendChild(child);
        }
        else {
            child = (Element) Validate.notNull(children.item(0));
        }

        return child;
    }

    /**
     * Find a child element
     *
     * @param parent Parent element
     * @param predicate Predicate used to select child
     * @return First child element if there is one that fulfills predicate
     */
    public static Optional<Element> find(Element parent, Predicate<@NonNull Element> predicate) {
        Optional<Element> child = children(parent).stream().filter(predicate).findFirst();
        return child;
    }

    /**
     * Append a child element
     *
     * @param parent Parent element
     * @param childName Name of child element
     * @return Appended child element
     */
    public static Element appendChild(Element parent, String childName) {
        @NonNull Document document = Validate.notNull(parent.getOwnerDocument(),
                "The element's document must not be null");
        Element child = document.createElement(childName);
        parent.appendChild(child);
        return child;
    }

    /**
     * Append a child element if the provided content is not <code>null</code>
     *
     * @param parent Parent element
     * @param childName Name of child element
     * @param childContent Content of child element
     * @return Appended child element or <code>null</code> if no element was appended
     */
    public static @Nullable Element appendChild(Element parent, String childName, @Nullable Object childContent) {
        Element child = null;

        if (childContent != null) {
            @NonNull Document document = Validate.notNull(parent.getOwnerDocument(),
                    "The element's document must not be null");
            child = document.createElement(childName);
            child.setNodeValue(childContent.toString());
            parent.appendChild(child);
        }

        return child;
    }

    /**
     * Set the value of a child element, creating the element if necessary, or deleting it if the value is
     * <code>null</code>
     *
     * @param parent Parent element
     * @param childName Name of child element
     * @param childContent Content of child element to set
     * @return Modified child element or <code>null</code> if no element was modified or of the element was deleted.
     */
    public static @Nullable Element setChildValue(Element parent, String childName, @Nullable Object childContent) {
        Element child = null;

        if (childContent == null) {
            NodeList children = parent.getElementsByTagName(childName);

            if (children.getLength() > 0) {
                child = (Element) children.item(0);
                parent.removeChild(child);
            }
        }
        else {
            child = child(parent, childName);
            child.setTextContent(childContent.toString());
        }

        return child;
    }

    /**
     * Get the children of an element
     *
     * @param parent Parent element
     * @return Child elements
     */
    public static List<@NonNull Element> children(Element parent) {
        return elements(parent.getChildNodes());
    }

    /**
     * Get the value of a child element
     *
     * @param parent Parent element
     * @param childName Name of child to get value of
     * @return Value of child element or <code>null</code> if the child element does not exist
     */
    public static @Nullable String childValue(Element parent, String childName) {
        String value = null;
        NodeList children = parent.getElementsByTagName(childName);

        if (children.getLength() > 0) {
            @NonNull Node child = Validate.notNull(children.item(0));
            value = child.getTextContent();
        }

        return value;
    }
}
