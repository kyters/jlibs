/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T <santhosh.tekuri@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.nblr;

import jlibs.core.io.FileUtil;
import jlibs.core.lang.ImpossibleException;
import jlibs.nblr.actions.EventAction;
import jlibs.nblr.actions.PublishAction;
import jlibs.nblr.editor.serialize.SyntaxBinding;
import jlibs.nblr.editor.serialize.SyntaxDocument;
import jlibs.nblr.matchers.Matcher;
import jlibs.nblr.rules.Edge;
import jlibs.nblr.rules.Node;
import jlibs.nblr.rules.Rule;
import jlibs.xml.sax.binding.BindingHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author Santhosh Kumar T
 */
public class Syntax{
    public final Map<String, Matcher> matchers = new LinkedHashMap<String, Matcher>();
    public Matcher add(String name, Matcher matcher){
        matcher.name = name;
        matchers.put(name, matcher);
        return matcher;
    }

    public final Map<String, Rule> rules = new LinkedHashMap<String, Rule>();
    
    public Rule add(String name, Rule rule){
        rule.name = name;
        rules.put(name, rule);
        updateRuleIDs();        
        return rule;
    }
    
    public void delete(Rule rule){
        rules.remove(rule.name);
        updateRuleIDs();
    }
    
    public void updateRuleIDs(){
        int id = 0;
        for(Rule r: rules.values())
            r.id = id++;
    }

    public String ruleProtypeWidth(){
        String lengthyRuleName = "XXXXXXXXX";
        for(Rule rule: rules.values()){
            if(rule.name.length()>lengthyRuleName.length())
                lengthyRuleName = rule.name;
        }
        return lengthyRuleName;
    }

    public List<Rule> usages(Rule rule){
        List<Rule> usages = new ArrayList<Rule>();
        for(Rule r: rules.values()){
            for(Edge edge: r.edges()){
                if(edge.ruleTarget!=null){
                    if(edge.ruleTarget.rule==rule && !usages.contains(r))
                        usages.add(r);
                }
            }
        }
        return usages;
    }

    public List<Rule> primaryRules(){
        List<Rule> primaryRules = new ArrayList<Rule>(rules.values());
        for(Rule r: rules.values()){
            for(Edge edge: r.edges()){
                if(edge.ruleTarget!=null){
                    if(edge.ruleTarget.rule!=r && edge.ruleTarget.node()==edge.ruleTarget.rule.node)
                        primaryRules.remove(edge.ruleTarget.rule);
                }
            }
        }
        return primaryRules;
    }

    public void computeBufferingStates(){
        BufferingDepths depths = new BufferingDepths();
        for(Rule rule: primaryRules())
            depths.calculate(rule);
        List<Rule> list = new ArrayList<Rule>(rules.values());
        list.removeAll(depths.ruleMap.keySet());

        while(!list.isEmpty()){
            Rule rule = list.get(0);
            depths.calculate(rule);
            list = new ArrayList<Rule>(rules.values());
            list.removeAll(depths.ruleMap.keySet());
        }
    }

    /*-------------------------------------------------[ Serialization ]---------------------------------------------------*/

    public void saveTo(File file) throws IOException{
        FileOutputStream fout = null;
        try{
            fout = new FileOutputStream(file);
            SyntaxDocument xml = new SyntaxDocument(new StreamResult(fout));
            xml.startDocument();
            xml.add(this);
            xml.endDocument();
        }catch(IOException ex){
            throw ex;
        }catch(Exception ex){
            throw new IOException(ex);
        }finally{
            try{
                if(fout!=null)
                    fout.close();
            }catch(IOException ex){
                ex.printStackTrace();
            }
        }
    }

    public static Syntax loadFrom(File file) throws IOException, SAXException, ParserConfigurationException{
        BindingHandler handler = new BindingHandler(SyntaxBinding.class);
        return (Syntax)handler.parse(new InputSource(new FileInputStream(file)));
    }

    public Syntax copy(){
        try{
            File tempFile = new File("temp/temp.syntax");
            FileUtil.mkdirs(tempFile.getParentFile());

            saveTo(tempFile);
            return Syntax.loadFrom(tempFile);
        }catch(Exception ex){
            throw new ImpossibleException(ex);
        }
    }

    /*-------------------------------------------------[ Actions ]---------------------------------------------------*/

    public Set<String> publishMethods(){
        Set<String> methods = new TreeSet<String>();
        for(Rule rule: rules.values()){
            for(Node node: rule.nodes()){
                if(node.action instanceof PublishAction)
                    methods.add(((PublishAction)node.action).name);
            }
        }
        return methods;
    }

    public Set<String> eventMethods(){
        Set<String> methods = new TreeSet<String>();
        for(Rule rule: rules.values()){
            for(Node node: rule.nodes()){
                if(node.action instanceof EventAction)
                    methods.add(((EventAction)node.action).name);
            }
        }
        return methods;
    }
}
