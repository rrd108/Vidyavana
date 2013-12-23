package hu.vidyavana.db.model;

import hu.vidyavana.db.api.Db;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.model.*;

@Entity
public class BookMeta
{
	@PrimaryKey
	public int id;
	
	public int[] paraEndDocIndexes;
	
	
	public static PrimaryIndex<Integer, BookMeta> pkIdx()
	{
		return Db.store().getPrimaryIndex(Integer.class, BookMeta.class);
	}
}
