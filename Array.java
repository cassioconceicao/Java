/*
 * Copyright (C) 2026 ctecinf.com.br
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.com.ctecinf.jetty;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Cássio Conceição
 * @since 01/04/2026
 * @version 2604
 * @see http://ctecinf.com.br/
 */
public class Array extends LinkedHashMap<Object, Object> {

    /**
     * The current pointer in the data
     */
    private int position;

    /**
     * The data to unserialize
     */
    private final String data;

    /**
     * The object history for resolving references
     */
    private final List history;

    /**
     * Constructor
     */
    public Array() {
        super();
        this.position = 0;
        this.data = "";
        this.history = new ArrayList();
    }

    /**
     * Constructor
     *
     * @param data The data to unserialize
     * @throws br.com.ctecinf.jetty.JettyException
     */
    public Array(String data) throws JettyException {
        super();
        this.position = 0;
        this.data = new String(data.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        this.history = new ArrayList();
        putAll(unserializeArray());
    }

    /**
     * Unserializes the next object in the data stream.
     *
     * @return The unserializes object
     * @throws br.com.ctecinf.jetty.JettyException
     */
    private Object unserializeObject() throws JettyException {
        Object result;
        switch (data.charAt(position)) {
            case 's' ->
                result = unserializeString();
            case 'i' ->
                result = unserializeNumber();
            case 'd' ->
                result = unserializeNumber();
            case 'b' ->
                result = unserializeBoolean();
            case 'N' ->
                result = unserializeNull();
            case 'a' -> {
                return unserializeArray();
            }
            case 'R' ->
                result = unserializeReference();
            default ->
                throw new JettyException("Unable to unserialize unknown type " + data.charAt(position));
        }
        history.add(result);
        return result;
    }

    /**
     * Unserializes the next object in the data stream into a String.
     *
     * @return The unserialized String
     */
    private String unserializeString() {
        int pos = data.indexOf(':', position + 2);
        int length = Integer.parseInt(data.substring(position + 2, pos));
        /**
         * *********************************************************************
         * Serialize PHP conta os acentos como caracter, então precisa diminuir
         * os acentos do tamanho da string para não pegar 'sujeira'
         */
        Number count = data.substring(pos + 2, (pos + 2 + length)).chars().filter(c -> "ÁÉÍÓÚáéíóúÀÈÌÒÙàèìòùÂÊÎÔÛâêîôûÃÑÕãñõÇç".indexOf(c) != -1).count();
        length = length - count.intValue();
        // *********************************************************************
        position = pos + length + 4;
        return data.substring(pos + 2, (pos + 2 + length));
    }

    /**
     * Unserializes the next object in the data stream into an Double.
     *
     * @return The unserialized Double
     */
    private Number unserializeNumber() {
        int pos = data.indexOf(';', position + 2);
        Double result = Double.valueOf(data.substring(position + 2, pos));
        position = pos + 1;
        return (Number) result;
    }

    /**
     * Unserializes the next object in the data stream as a reference.
     *
     * @return The unserialized reference
     */
    private Object unserializeReference() {
        int pos = data.indexOf(';', position + 2);
        int get = Integer.parseInt(data.substring(position + 2, pos));
        position = pos + 1;
        return history.get(get - 1);
    }

    /**
     * Unserializes the next object in the data stream into a Boolean.
     *
     * @return The unserialized Boolean
     */
    private Boolean unserializeBoolean() {
        Boolean result = data.charAt(position + 2) == '1';
        position += 4;
        return result;
    }

    /**
     * Unserializes the next object in the data stream into a Null
     *
     * @return The unserialized Null
     */
    private Object unserializeNull() {
        position += 2;
        return null;
    }

    /**
     * Unserializes the next object in the data stream into an array. This
     * method returns an ArrayList if the unserialized array has numerical keys
     * starting with 0 or a HashMap otherwise.
     *
     * @return The unserialized array
     */
    private Array unserializeArray() throws JettyException {
        int pos = data.indexOf(':', position + 2);
        int max = Integer.parseInt(data.substring(position + 2, pos));
        position = pos + 2;
        Array array = new Array();
        for (int i = 0; i < max; i++) {
            Object key = unserializeObject();
            history.remove(history.size() - 1);
            Object value = unserializeObject();
            array.put(key instanceof Number ? ((Number) key).intValue() : key, value);
        }
        history.add(array);
        position++;
        return array;
    }

    /**
     * Verifica se valor é um Array
     *
     * @param value
     * @return boolean
     */
    private boolean isArray(Object value) {
        return value != null && value instanceof Array;
    }

    /**
     * Verifica se valor é um Number
     *
     * @param value
     * @return boolean
     */
    private boolean isNumber(Object value) {
        return value != null && value instanceof Number;
    }

    /**
     * Returns the unserialized object of the specified PHP serialize format
     * string.The returned object is wrapped in a Mixed object allowing easy
     * conversion to any data type needed. This wrapping is needed because PHP
     * is a loosely typed language and it is quite propable that a boolean is
     * sometimes a int or a string. So with the Mixed wrapper object you can
     * easily decide on your own how to interpret the unserialized data.
     *
     * @param data The serialized data
     * @return The unserialized object
     * @throws br.com.ctecinf.jetty.JettyException
     */
    public static Array unserialize(String data) throws JettyException {
        return new Array(data);
    }

    /**
     * Returns mixed value as a string.
     *
     * @param id
     * @return Mixed value as a string
     */
    public String getString(Object id) {
        return get(id) == null ? null : get(id).toString();
    }

    /**
     * Returns the value as an array.If value is not an array then null is
     * returned.
     *
     * @param id
     * @return The value as an array
     */
    public Array getArray(Object id) {
        Object value = get(id);
        if (value == null) {
            return null;
        } else if (isArray(value)) {
            return (Array) value;
        } else {
            return null;
        }
    }

    /**
     * Returns mixed value as a Number.If value is a boolean value then 0 is
     * returned if value is false and 1 is returned if value is true. For any
     * other data type a conversion is tried and 0 is returned if this fails.
     *
     * @param id
     * @return Mixed value as a Number
     */
    public Number getNumber(Object id) {
        Object value = get(id);
        if (value == null) {
            return 0;
        } else if (isNumber(value)) {
            return (Number) value;
        } else {
            return null;
        }
    }

    /**
     * Returns true | false
     *
     * @param id
     * @return Mixed value as a boolean
     */
    public Boolean getBoolean(Object id) {
        Object value = get(id);
        if (value == null) {
            return false;
        } else if (value.getClass().isAssignableFrom(Boolean.class)) {
            return Boolean.valueOf(value.toString());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "{" + entrySet().stream().map(entry -> "\"" + entry.getKey() + "\": " + (isArray(entry.getValue()) ? entry.getValue() : "\"" + entry.getValue().toString() + "\"")).collect(Collectors.joining(", ")) + "}";
    }

}
