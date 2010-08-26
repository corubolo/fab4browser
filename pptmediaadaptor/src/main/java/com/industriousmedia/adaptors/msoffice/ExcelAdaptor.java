package com.industriousmedia.adaptors.msoffice;

import multivalent.INode;
import multivalent.MediaAdaptor;
import org.apache.poi.ss.examples.html.ToHtml;

import java.io.IOException;
import java.io.InputStream;

public class ExcelAdaptor extends MediaAdaptor {
    @Override
    public Object parse(INode parent) throws Exception {
        return parseHelper(toHTML(), "HTML", getLayer(), parent);
    }

    private String toHTML() throws IOException {
        InputStream in = null;
        try {
            StringBuilder sb = new StringBuilder();
            in = getInputUni().getInputStream();
            ToHtml toHtml = ToHtml.create(in, sb);
            toHtml.printPage();
            //System.out.println(sb);
            return sb.toString();
        } finally {
            if (in != null)
                in.close();
        }
    }

//    {
//        InputStreamTee in = null;
//        try {
//            in = getInputUni().getInputStream();
//            SViewerPanel viewer = new SViewerPanel(in, false);
//            // The "new" adds it to the tree
//            return new LeafSwing("spreadsheet", null, parent, viewer);
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw e;
//        } finally {
//            if (in != null)
//                in.close();
//        }
//    }
}