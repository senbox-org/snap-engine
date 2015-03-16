package org.esa.beam.dataio;

import org.esa.beam.framework.dataio.ProductIOException;
import org.junit.Ignore;
import org.junit.Test;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.h4.H4GRImage;
import ncsa.hdf.object.h5.H5ScalarDS;
import org.esa.beam.framework.dataio.ProductIOException;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.List;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 11.03.2015
 * Time: 16:31
 *
 * @author olafd
 */
public class ProbaVProductReaderTest {

    @Test
    @Ignore
    public void testReadH5() throws ProductIOException {
        ProbaVProductReaderPlugIn.loadHdf5Lib(ProbaVProductReaderPlugIn.class);
        try {
            H5.H5open();
            String path = "C:\\Users\\olafd\\proba_v_reader\\PROBAV_L1C_20131025_115650_2_V003.HDF5";
            int file_id = H5.H5Fopen(path, HDF5Constants.H5F_ACC_RDONLY, HDF5Constants.H5P_DEFAULT);
            System.out.println("file_id = " + file_id);
        } catch (HDF5LibraryException e) {
            throw new ProductIOException(createErrorMessage(e));
        }

        try {
            H5.H5close();
        } catch (HDF5LibraryException e) {
            throw new ProductIOException(createErrorMessage(e));
        }
    }

    @Test
    @Ignore
    public void testReadH5_2() throws Exception {
        final Class<?> aClass = ProbaVProductReaderPlugIn.loadHdf5Lib(ProbaVProductReaderPlugIn.class);
//        String path = "C:\\Users\\olafd\\bc\\proba-v-reader\\PROBAV_L1C_20131025_115650_2_V003.HDF5";
        String path = "C:\\Users\\olafd\\proba_v_reader\\HDF5_LSASAF_MSG_ALBEDO_Euro_200601020000";
        FileFormat h5FileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF5);
        FileFormat h5File = h5FileFormat.createInstance(path, FileFormat.READ);

        final int h5FileId = h5File.open();
        System.out.println("h5FileId = " + h5FileId);

        final TreeNode rootNode = h5File.getRootNode();
        if (rootNode != null) {
//            printNode(rootNode, "    ");
            final TreeNode l1cBlueToaChild = rootNode.getChildAt(2).getChildAt(0).getChildAt(1);
            System.out.println("Child: " + l1cBlueToaChild.toString());
            final H5ScalarDS scalarDS = (H5ScalarDS) ((DefaultMutableTreeNode) l1cBlueToaChild).getUserObject();
            scalarDS.open();
            scalarDS.read();
            final short[] data = (short[]) scalarDS.getData();   // the data we see in HDFView :-)
            System.out.println("ds datatype = " + scalarDS.getDatatype());
            final List<Attribute> metadata = scalarDS.getMetadata();
            for (Attribute attribute : metadata) {
                System.out.println("attribute name = " + attribute.getName());
                System.out.println("attribute value = " + ProbaVUtils.getAttributeValue(attribute));
            }

        }

        h5File.close();
    }

    // print out the data object recusively
    private static void printNode(javax.swing.tree.TreeNode node, String indent) {
        System.out.println(indent + node);

        int n = node.getChildCount();
        for (int i = 0; i < n; i++) {
            printNode(node.getChildAt(i), indent + "    ");
        }
    }

    private String createErrorMessage(HDF5Exception e) {
        return "HDF library error: " + e.getMessage();
    }

}
