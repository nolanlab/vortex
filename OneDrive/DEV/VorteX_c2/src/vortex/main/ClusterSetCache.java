/*
 * Copyright (C) 2015 Nikolay
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package vortex.main;

import clustering.ClusterSet;
import java.sql.SQLException;
import java.util.HashMap;
import clustering.Dataset;
import util.logger;
import vortex.util.ConnectionManager;

/**
 *
 * @author Nikolay
 */
public class ClusterSetCache {

    private static final HashMap<Dataset, HashMap<Integer, ClusterSet>> cache = new HashMap<>();
    
    public static void remove(Dataset ds, int CSID){
        if(cache.get(ds)!=null){
            cache.get(ds).remove(CSID);
        }
    }
    
    public static ClusterSet getInstance(Dataset ds, int CSID) {
        if (cache.get(ds) == null) {
            HashMap<Integer, ClusterSet> hm = new HashMap<>();
            cache.put(ds, hm);
            try {
                ClusterSet CS = ConnectionManager.getStorageEngine().loadClusterSet(CSID, ds);
                hm.put(CSID, CS);
                return CS;
            } catch (SQLException e) {
                logger.showException(e);
            }

        } else {
            if (cache.get(ds).get(CSID) == null) {
                try {
                    ClusterSet CS = ConnectionManager.getStorageEngine().loadClusterSet(CSID, ds);
                    cache.get(ds).put(CSID, CS);
                    return CS;
                } catch (SQLException e) {
                    logger.showException(e);
                }
            } else {
                return cache.get(ds).get(CSID);
            }
        }
        return null;
    }
}
