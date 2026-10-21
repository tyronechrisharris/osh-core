/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.h2;

import static org.junit.Assert.assertEquals;
import com.google.common.collect.Range;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import org.h2.mvstore.MVStore;
import org.junit.After;
import org.junit.Test;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.impl.datastore.AbstractTestObsStore;
import org.sensorhub.impl.datastore.h2.MVDatabaseConfig.IdProviderType;


public class TestMVObsStore extends AbstractTestObsStore<MVObsStoreImpl>
{
    private static String DB_FILE_PREFIX = "test-mvobs-";
    protected File dbFile;
    protected MVStore mvStore;
    
    
    protected MVObsStoreImpl initStore() throws Exception
    {
        dbFile = File.createTempFile(DB_FILE_PREFIX, ".dat");
        dbFile.deleteOnExit();
        return openMVStore();
    }
    
    
    protected void forceReadBackFromStorage()
    {
        this.obsStore = openMVStore();
    }
    
    
    private MVObsStoreImpl openMVStore()
    {
        if (mvStore != null)
        {
            mvStore.commit();
            mvStore.close();
            System.out.println("MVStore flushed to disk");
        }
        
        mvStore = new MVStore.Builder()
                .fileName(dbFile.getAbsolutePath())
                .autoCommitBufferSize(10)
                .cacheSize(1)
                .open();
        
        return MVObsStoreImpl.open(mvStore, DATABASE_NUM, IdProviderType.SEQUENTIAL,
            MVDataStoreInfo.builder()
                .withName(OBS_DATASTORE_NAME)
                .build());
    }
    
    
    @After
    public void cleanup() throws Exception
    {
        if (mvStore != null)
            mvStore.close();
        
        if (dbFile != null) 
        {
            System.out.println("DB file size was " + dbFile.length()/1024 + "KB\n");
            
            Files.list(dbFile.toPath().getParent())
                 .filter(f -> f.getFileName().toString().startsWith(DB_FILE_PREFIX))
                 .forEach(f -> f.toFile().delete());
        }            
    }
    
    
    @Test
    public void testGetNumRecordsTwoDataStreams() throws Exception
    {
        super.testGetNumRecordsTwoDataStreams();
        
        // check that 2 series were created
        assertEquals(2, obsStore.obsSeriesMainIndex.size());
    }


    @Test
    public void testOpenAndClosedPhenomenonTimeBoundsForSelectAndRemove() throws Exception
    {
        var dataStreamID = addSimpleDataStream(bigId(21), "bounded-output");
        Instant first = Instant.parse("2026-10-20T00:00:00Z");
        Instant second = first.plusSeconds(1);
        Instant third = first.plusSeconds(2);
        Instant fourth = first.plusSeconds(3);
        addSimpleObsWithoutResultTime(dataStreamID, org.sensorhub.api.common.BigId.NONE,
            first, 4, 1_000);
        forceReadBackFromStorage();

        ObsFilter closedOpen = new ObsFilter.Builder()
            .withDataStreams(dataStreamID)
            .withPhenomenonTime(new TemporalFilter.Builder()
                .withRange(Range.closedOpen(first, third))
                .build())
            .build();
        try (var selected = obsStore.select(closedOpen))
        {
            assertEquals(List.of(first, second), selected
                .map(obs -> obs.getPhenomenonTime())
                .toList());
        }

        ObsFilter openClosed = new ObsFilter.Builder()
            .withDataStreams(dataStreamID)
            .withPhenomenonTime(new TemporalFilter.Builder()
                .withRange(Range.openClosed(first, third))
                .build())
            .build();
        try (var selected = obsStore.select(openClosed))
        {
            assertEquals(List.of(second, third), selected
                .map(obs -> obs.getPhenomenonTime())
                .toList());
        }

        assertEquals(2L, obsStore.removeEntries(closedOpen));
        try (var remaining = obsStore.select(new ObsFilter.Builder()
            .withDataStreams(dataStreamID)
            .build()))
        {
            assertEquals(List.of(third, fourth), remaining
                .map(obs -> obs.getPhenomenonTime())
                .toList());
        }
    }

}
