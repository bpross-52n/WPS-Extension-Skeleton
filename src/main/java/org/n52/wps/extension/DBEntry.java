package org.n52.wps.extension;

import java.util.Map;

public class DBEntry {

    private int dg;
    
    private String name;
    
    private Map<String, Object> attributes;
    
    private String[] attributeArray;
    
    public DBEntry() {
    }
    
    public DBEntry(int dg, String name, String[] attributeArray) {
        this.dg = dg;
        this.name = name.trim();
        this.attributeArray = attributeArray;
    }

    public int getDg() {
        return dg;
    }

    public void setDg(int dg) {
        this.dg = dg;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String[] getAttributeArray() {
        return attributeArray;
    }

    public void setAttributeArray(String[] attributeArray) {
        this.attributeArray = attributeArray;
    }
    
    @Override
    public String toString() {
        
        String[] attributenames = FusionUtil.attributeNames;
        
        String lineSeparator = System.getProperty("line.separator");
        
        StringBuilder stringBuilder = new StringBuilder();
        
        stringBuilder.append("DG: " + dg + lineSeparator);
        stringBuilder.append("Name: " + name + lineSeparator);
        stringBuilder.append(attributenames[0] + " "  + attributeArray[0] + lineSeparator);
        stringBuilder.append(attributenames[1] + " "  + attributeArray[1] + lineSeparator);
        stringBuilder.append(attributenames[2] + " "  + attributeArray[2] + lineSeparator);
        stringBuilder.append(attributenames[3] + " "  + attributeArray[3] + lineSeparator);
        
        return stringBuilder.toString();
    }
}
