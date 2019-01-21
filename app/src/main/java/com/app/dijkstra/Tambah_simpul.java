package com.app.dijkstra;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
			//added node
public class Tambah_simpul{
	
	protected Cursor cursor;
	SQLHelper dbHelper;

	String[][] modif_graph = new String[100][100];
	String simpul_lama = "";
	int simpul_baru;
	
	String simpul_tujuan_ = "";
			//destination node
	
	/*
	 * @fungsi
	 *  meyisipkan simpul baru 
	 *  misal simpul 5-4, disisipkan menjadi 5-6-4
	 *  dan simpul 4-5, disisipkan menjadi 4-6-5
	 * @parameter
	 *  nodes0 : misal {"nodes": "5-4"} maka nodes_awal0 = 5
	 *  nodes1 : misal {"nodes": "5-4"} maka nodes_awal1 = 4
	 *  index_koordinat_json : index array koordinat di JSON
	 *  context : MainActivity.context
	 *  graph[][] : array untuk menampung graph dari DB
	 *   			example output : graph[5][0] = 4->439.281
	 *   							 graph[6][0] = 1->216.281 
	 *  increase_row_id : row id DB yg baru
	 * @return
	 *  simpul_lama = nodes0 + "-" + nodes1
	 *  simpul_baru = simpul awal
	 *  graph[][]
	 */
	//          douoble node
	public void dobelSimpul(int nodes0, int nodes1, 
							int index_koordinat_json, Context context, 
							String[][] graph, int increase_row_id
						) throws JSONException{
		
		// read DB
		SQLHelper dbHelper = new SQLHelper(context);
		SQLiteDatabase dbRead = dbHelper.getReadableDatabase();
		//create insert to DB
		//buat insert ke DB
		SQLiteDatabase dbInsert = dbHelper.getWritableDatabase();
		
		//count node that original past (5-4), not that double(4-5)
		// HITUNG SIMPUL YANG ASLI DULU (5-4), BUKAN YANG DOBEL (4-5)
		//==============================================================		
		String index_kolom_graph = "";

		// cari index kolomnya nodes1 (4) dari graph[baris][index kolom]
		//search index column nodes 1(4) from graph[line][index column]
		for(int l = 0; l < 100; l++){

			if(graph[nodes0][l] != null){
				
				String simpulAwal = graph[nodes0][l]; // [5][0] = 4->721.666
				       //(nodeStart)
				// 4->721.666
				String [] explode = simpulAwal.split("->");
				
				simpul_tujuan_ = explode[0]; // 4
				//node_name_
				// jika 4 == 4 (node1)
				//if
				if(simpul_tujuan_.trim().equals( String.valueOf(nodes1).trim()) ){
					
					// index kolom; example graph[baris][kolom]
					// index column; example graph[line][column]
					index_kolom_graph = String.valueOf(l);
				}
							
			}else break;
						
		}// for
		
		// index dari graph[baris][kolom] yang akan di edit
		int baris = nodes0;
		//(line)
		int kolom = Integer.parseInt(index_kolom_graph);
		//(column)

		// ambil koordinatnya dari simpul 5-4
		//take coordinates from node 5-4
		cursor = dbInsert.rawQuery("SELECT jalur FROM graph where simpul_awal = "+ nodes0 +" and simpul_tujuan = "+ nodes1, null);
											// (path)                  (node_start)                    (node_name)
		cursor.moveToFirst();
		cursor.moveToPosition(0);

		// --
		// get coordinates JSON
		String json_coordinates = cursor.getString(0).toString();		
		JSONObject jObject = new JSONObject(json_coordinates);		
		JSONArray jArrCoordinates = jObject.getJSONArray("coordinates");
		// --
		
		// cari maksimal simpul, (buat penomoran simpul baru)
		//search maximum node, (create numbering node new)
		cursor = dbRead.rawQuery("SELECT max(simpul_awal), max(simpul_tujuan) FROM graph", null);
												//(node_start)    (node_name)
		cursor.moveToFirst();
		int max_simpul_db		= 0;
		int max_simpulAwal_db 	= Integer.parseInt(cursor.getString(0).toString());			
		int max_simpulTujuan_db = Integer.parseInt(cursor.getString(1).toString());
		if(max_simpulAwal_db >= max_simpulTujuan_db){
			//(node_start)			(node_name)
			max_simpul_db = max_simpulAwal_db;
		}else{
			max_simpul_db = max_simpulTujuan_db;
		}
		
		// pecah koordinat dari AWAL->TENGAH
		//broken coordinate from early->Central
		int limit = index_koordinat_json;		
		Count_Bobot_Tambah_Simpul ct = new Count_Bobot_Tambah_Simpul();
		//(count_weight__Addit_node)
		ct.Count_Bobot_Tambah_Simpul(0, limit, jArrCoordinates); // 0, koordinat tengah, jSON coordinates
																			//coordinate middle
		//replace array graph[5][0] = 6->888.6
		graph[baris][kolom] = (max_simpul_db+1)+"->"+ct.bobot;
		//[line][column]							(weight)
		
		int start_loop = 0;
		// buat dan simpan (new record) json koordinat yang baru ke DB
		//create and save it (new record) json coordinate that new to DB
		createAndSave_NewJsonCoordinate(start_loop, limit, jArrCoordinates, increase_row_id, baris, (max_simpul_db + 1), ct.bobot,
										dbInsert, dbRead); // 501 : index record baru
																				//new
		
		// reset bobot
		//reset weight
		ct.bobot = 0;

			
		// pecah koordinat dari TENGAH->AKHIR
		//broken coordinate from middle->end
		int start_loop1 = index_koordinat_json;
		int limit1 = (jArrCoordinates.length() - 1); // - 1 karena array mulai dari 0
															//because array start from 0
		ct.Count_Bobot_Tambah_Simpul(index_koordinat_json, limit1, jArrCoordinates); // coordinate tengah sampai akhir
		
																					// coordinate middle to end
		// new array graph[6][0] = 4->777.4
		graph[(max_simpul_db+1)][0] = nodes1 + "->" + ct.bobot; //didefinisikan [0] karena index baru di graph[][]
																//defined [0] because index new in graph[] []
		// buat dan simpan (new record) json koordinate yang baru ke DB
		createAndSave_NewJsonCoordinate(start_loop1, limit1, jArrCoordinates, ++increase_row_id, (max_simpul_db + 1), nodes1, ct.bobot,
										dbInsert, dbRead); // 502 : index record baru
																				//new
		
		// reset bobot
				//weight
		ct.bobot = 0;


		// HITUNG SIMPUL YANG DOBEL (4-5), BUKAN YANG ASLI (5-4)
		//==============================================================
		//count node that double (4-5), not that original (5-4)
		String index_kolom_graph1 = "";
		String nodes_inside_kolom = "";
		//behind, nodes0 so nodes1; example (5-4) so (4-5)
		// dibalik, nodes0 jadi nodes1; example (5-4) jadi (4-5)
		int t_nodes0 = nodes1; // 4
		int t_nodes1 = nodes0; // 5
		//search index column from graph[4] [index column]
		// cari index kolomnya dari graph[4][index kolomnya]
		for(int l = 0; l < 100; l++){

			if(graph[t_nodes0][l] != null){
				//== get destination node,example: 5->9585.340
				// == dapatkan simpul tujuan, example : 5->9585.340
				String simpulAwal = graph[t_nodes0][l];
				String [] explode1 = simpulAwal.split("->");
				
				nodes_inside_kolom = explode1[0];
				
				if(nodes_inside_kolom.trim().equals( String.valueOf(t_nodes1)) ){
					index_kolom_graph1 = String.valueOf(l);
				}
				
			}else break;
		}//for
			
		// index from graph[line1][column1] that will in edit
		// index dari graph[baris1][kolom1] yang akan di edit
		int baris1 = t_nodes0;
		int kolom1 = Integer.parseInt(index_kolom_graph1);
		// take coordinate from node 4-5
		// ambil koordinatnya dari simpul 4-5
		cursor = dbRead.rawQuery("SELECT jalur FROM graph where simpul_awal = "+t_nodes0+" and simpul_tujuan = "+t_nodes1, null);
											//(path)				early node						destination node
		cursor.moveToFirst();
		cursor.moveToPosition(0);

		// --
		// get coordinates JSON from DB
		String json1 = cursor.getString(0).toString();
		JSONObject jObject1 = new JSONObject(json1);
		JSONArray jArrCoordinates1 = jObject1.getJSONArray("coordinates");
		// --
		//broken coordinate from early->destination
		// pecah koordinat dari AWAL->TENGAH
		int index_dobel_koordinat_json = ( (jArrCoordinates1.length()-1) - index_koordinat_json );
		ct.Count_Bobot_Tambah_Simpul(0, index_dobel_koordinat_json, jArrCoordinates1); // 0, koordinat awal ke tengah, JSONArray coordinate

		//replace array graph[4][0] = 6->777.4
		graph[baris1][kolom1] = (max_simpul_db+1)+"->"+ct.bobot;
		
		//create and save it(new record) json coordinate that new to DB
		// buat dan simpan (new record) json koordinate yang baru ke DB
		int start_loop2 = 0;
		createAndSave_NewJsonCoordinate(start_loop2, index_dobel_koordinat_json, jArrCoordinates1, ++increase_row_id, baris1, (max_simpul_db + 1), ct.bobot,
										dbInsert, dbRead); // 503 : index record baru
																//index record new
				
		// reset bobot
		ct.bobot = 0;

				//broken coordinate from middle->end
		// pecah koordinat dari TENGAH->AKHIR
		int limit2 = (jArrCoordinates1.length() - 1); // - 1 karena array mulai dari 0
														//because array start from 0
		ct.Count_Bobot_Tambah_Simpul(index_dobel_koordinat_json, limit2, jArrCoordinates1); // koordinat tengah sampai akhir
																							//coordinate middle to end
		//replace array graph[6][1] = 5->888.6
		graph[(max_simpul_db+1)][1] = t_nodes1+"->"+ct.bobot; // didefinisikan [1] karena sdh ada index 0 di graph[][]
																//defined [1] because sdh there is index 0 in graph[][]
		// buat dan simpan (new record) json koordinate yang baru ke DB
		createAndSave_NewJsonCoordinate(index_dobel_koordinat_json, limit2, jArrCoordinates1, ++increase_row_id, (max_simpul_db + 1), t_nodes1, ct.bobot,
										dbInsert, dbRead); // 503 : index record baru	
																//				new
		
		
		// return
		simpul_lama = nodes0 + "-" + nodes1;
		simpul_baru = (max_simpul_db + 1);
		modif_graph = graph; // graph[][]
		
	}// public void dobelSimpul

					//doublenode
	
	/*
	 * @fungsi
	 *  meyisipkan simpul baru 
	 *  misal simpul 5-4, disisipkan menjadi 5-6-4
	 * @parameter
	 *  nodes_awal0 : misal {"nodes": "5-4"} maka nodes_awal0 = 5
	 *  nodes_awal1 : misal {"nodes": "5-4"} maka nodes_awal1 = 4
	 *  index_koordinat_json : index array koordinat di JSON
	 *  context : MainActivity.context
	 *  graph[][] : array untuk menampung graph dari DB
	 *   			example output : graph[5][0] = 4->439.281
	 *   							 graph[6][0] = 1->216.281 
	 *  increase_row_id : row id DB yg baru
	 * @return
	 *  simpul_baru : simpul akhir
	 *  graph[][]
	 */	
	public void singleSimpul(int nodes0, int nodes1, int index_koordinat_json, 
								Context context, String[][] graph, int increase_row_id) throws JSONException{
	
		// read DB
		SQLHelper dbHelper = new SQLHelper(context);
		SQLiteDatabase dbRead = dbHelper.getReadableDatabase();
		
		//buat insert ke DB
		//create insert to DB
		SQLiteDatabase dbInsert = dbHelper.getWritableDatabase();
		
		//count node that original (5-4)
		// HITUNG SIMPUL YANG ASLI (5-4)
		//==============================================================
		
		String index_kolom_graph = "";
		// search index column nodes_end1 (4) from graph[line][column]
		// cari index kolomnya nodes_akhir1 (4) dari graph[baris][kolom]
		for(int l = 0; l < 100; l++){

			if(graph[nodes0][l] != null){

				String simpulAwal = graph[nodes0][l]; // [5][0] = 4->721.666
				String [] explode = simpulAwal.split("->");
									//earlynode
				// 6->721.666
				String value_node_array = explode[0];
				
				// jika 4 == 4 (node_akhir1)
				if( value_node_array.trim().equals(String.valueOf(nodes1).trim()) ){
					
					// index kolom; example graph[baris][kolom]
					index_kolom_graph = String.valueOf(l);
				}
				
			}else break;
		}//for

		// index from graph[line][column] that will in edit
		// index dari graph[baris][kolom] yang akan di edit
		int baris = nodes0;
		int kolom = Integer.parseInt(index_kolom_graph);

		//take coordinates from node 3-5
		//ambil koordinatnya dari simpul 3-6
		cursor = dbRead.rawQuery("SELECT jalur FROM graph where simpul_awal = "+nodes0+" and simpul_tujuan = "+nodes1, null);
		cursor.moveToFirst();
		cursor.moveToPosition(0);
		
		// --
		// get coordinates JSON
		String json_coordinates = cursor.getString(0).toString();		
		JSONObject jObject = new JSONObject(json_coordinates);		
		JSONArray jArrCoordinates = jObject.getJSONArray("coordinates");
		// --
		// search maximum node,(create numbering node new
		// cari maksimal simpul, (buat penomoran simpul baru)
		cursor = dbRead.rawQuery("SELECT max(simpul_awal) FROM graph", null);
		cursor.moveToFirst();
		int max_simpul_db = Integer.parseInt(cursor.getString(0).toString());			
	
		//broken coordinate from early->middle
		// pecah koordinat dari AWAL->TENGAH	
		System.out.println("single awal->tengah");
		int limit = index_koordinat_json;	
		Count_Bobot_Tambah_Simpul ct = new Count_Bobot_Tambah_Simpul();
		ct.Count_Bobot_Tambah_Simpul(0, limit, jArrCoordinates); // 0, koordinat tengah, jSON coordinates
																				//	middle
		//replace array graph[5][0] = 6->888.6
		graph[baris][kolom] = (max_simpul_db+1)+"->"+ct.bobot;
		//		[line][column]
		
		int start_loop = 0;		
		//create and save it(new record) json coordinate that new to DB
		// buat dan simpan (new record) json koordinat yang baru ke DB
		createAndSave_NewJsonCoordinate(start_loop, limit, jArrCoordinates, increase_row_id, baris, (max_simpul_db + 1), ct.bobot,
										dbInsert, dbRead); // 501 : index record baru
															// index record new
		// reset bobot
		//			weight
		ct.bobot = 0;

		//broken coordinate from middle->end
		// pecah koordinat dari TENGAH->AKHIR
		int start_loop1 = index_koordinat_json; // - 1 karena array mulai dari 0
														//create array start from 0
		int limit1 = (jArrCoordinates.length() - 1); // - 1 karena array mulai dari 0
		ct.Count_Bobot_Tambah_Simpul(index_koordinat_json, limit1, jArrCoordinates); // coordinate tengah sampai akhir
		
																					//	coordinate middle to end
		// new array graph[6][0] = 4->777.4
		graph[(max_simpul_db+1)][0] = nodes1 + "->" + ct.bobot; //didefinisikan [0] karena index baru di graph[][]
		
		// buat dan simpan (new record) json koordinate yang baru ke DB
		//create and save it (new record) json coordinate that new to DB
		createAndSave_NewJsonCoordinate(start_loop1, limit1, jArrCoordinates, ++increase_row_id, (max_simpul_db + 1), nodes1, ct.bobot,
										dbInsert, dbRead); // 502 : index record baru
																				//new
		// return
		simpul_lama = nodes0 + "-" + nodes1;
		simpul_baru = (max_simpul_db + 1);
		modif_graph = graph; // graph[][]
	}
	

	/* @fungsi
	 *  Membuat dan menyimpan coordinates baru dalam bentuk JSON ke DB
	 * @parameter 
	 *  mulai : mulai looping, misal 0
	 * 	limit : index array koordinat, misal i[7] maka limit = 7
	 *  jArrCoordinates : Koordinat dari DB dalam bentuk JSONArray
	 *  new_id : id record baru
	 *  //baris : baris multidimensi array, misal i[baris][kolom]
	 *  //max_simpul_db : jumlah max record pada tabel graph
	 *  new_bobot : bobot baru dari pemecahan koordinat jalur
	 *  dbInsert : insert ke database
	 *  dbRead : baca record database
	 * @return 
	 *  no return
	 */
	public void createAndSave_NewJsonCoordinate(int mulai, int limit, JSONArray jArrCoordinates,
													 int new_id, int field_simpul_awal, int field_simpul_akhir, double new_bobot,
													SQLiteDatabase dbInsert, SQLiteDatabase dbRead) throws JSONException{
		
		// JSON for save new coordinate
		JSONObject json_baru = new JSONObject();
		JSONArray new_root_coordinates = new JSONArray();
		//looping from coordinate early to coordinate middle
		// looping dari coordinate awal sampai ke coordinate tengah
		// atau
		//or
		//looping from coordinate middle to coordinate end
		// looping dari coordinate tengah sampai ke coordinate akhir
		// then, move old coordinate to new coordinate
		for(int ne = mulai; ne <= limit; ne++){
			
			JSONArray latlng = jArrCoordinates.getJSONArray(ne);
			double new_lat = latlng.getDouble(0);
			double new_lng = latlng.getDouble(1);
			
			JSONArray new_list_coordinates = new JSONArray();
			new_list_coordinates.put(new_lat);
			new_list_coordinates.put(new_lng);
			
			// coordinates
			new_root_coordinates.put(new_list_coordinates);			
		}


		// nodes
		JSONArray nodes = new JSONArray();
		String gabung_nodes = String.valueOf(field_simpul_awal) + '-' + String.valueOf(field_simpul_akhir);
		nodes.put(gabung_nodes);
		
		// distance_metres
		JSONArray distance_metres = new JSONArray();
		distance_metres.put(new_bobot);
		
		
		// create new JSON
		json_baru.put("nodes", nodes);
		json_baru.put("coordinates", new_root_coordinates);
		json_baru.put("distance_metres", distance_metres);
		
		String jalur_baru = json_baru.toString();
		System.out.println(jalur_baru);		
		//insert new node
		// INSERT SIMPUUUUUULL BARU 
		ContentValues newCon = new ContentValues();
		newCon.put("id", new_id);
		newCon.put("simpul_awal", field_simpul_awal);	
		newCon.put("simpul_tujuan", field_simpul_akhir);
		newCon.put("jalur", jalur_baru);	
		newCon.put("bobot", new_bobot);	
		dbInsert.insert("graph", null, newCon);
		
	}
}