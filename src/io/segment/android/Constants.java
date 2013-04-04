package io.segment.android;

public class Constants {

	public static final String PACKAGE_NAME = Constants.class.getPackage().getName();
	
	public static class Database {

		public static final int VERSION = 1;
		
		public static final String NAME = PACKAGE_NAME;
		
		public static class PayloadTable {
		
			public static final String NAME = "payload_table";
			
			public static final String[] FIELD_NAMES = new String[] { 
				Fields.Id.NAME,
				Fields.Payload.NAME 
			};
			
			public static class Fields {
				
				public static class Id {
					
					public static final String NAME = "id";
					
					/**
					 *  INTEGER PRIMARY KEY AUTOINCREMENT means index is monotonically
					 *  increasing, regardless of removals
					 */
					public static final String TYPE = "INTEGER PRIMARY KEY AUTOINCREMENT";
				}
				
				public static class Payload {
					
					public static final String NAME = "payload";
					
					public static final String TYPE = " TEXT";
				}
				
			}
			
		}
	}
	
	
}
