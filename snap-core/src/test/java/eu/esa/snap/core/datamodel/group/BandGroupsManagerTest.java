package eu.esa.snap.core.datamodel.group;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

public class BandGroupsManagerTest {

    @Test
    @STTM("SNAP-3702")
    public void testGetInstance_empty() {
        BandGroupsManager bandGroupsManager = BandGroupsManager.getInstance();
    }

    // create with file in user dir
    // get groups
    // get groupsForProduct
    // add group
    // add group - name exists
    // remove group
    // remove group - name does not exist
    // add groups from Product
    // remove groups from Product
    // save
    // save only stores user-defined groups
}
