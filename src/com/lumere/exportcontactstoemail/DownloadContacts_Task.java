package com.lumere.exportcontactstoemail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.text.TextUtils;
import android.util.Log;

public class DownloadContacts_Task extends AsyncTask<String, Float, File[]> {
	private ContentResolver content_resolver;
	private Activity activity;
	private Context context;

	public DownloadContacts_Task(Activity activity, ContentResolver cr,
			Context context) {
		this.onAttach(activity);
		this.content_resolver = cr;
		this.context = context;
	}

	public void onAttach(Activity activity) {
		this.activity = activity;
	}

	public void onDetach() {
		// nullify activity reference
		this.activity = null;
	}

	protected void onPreExecute() {
		if (this.activity != null) {
			((MainActivity) activity).showProgressBar();
		}
	}

	@Override
	protected File[] doInBackground(String... params) {

		FileOutputStream fos_csv = null;
		FileOutputStream fos_txt = null;

		try {
			fos_csv = this.context.openFileOutput("contacts.csv",
					Context.MODE_PRIVATE);
			fos_txt = this.context.openFileOutput("contacts.txt",
					Context.MODE_PRIVATE);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		Cursor contacts_cursor = this.content_resolver.query(
				ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

		Cursor phone_cursor = null, email_cursor = null;

		float total_contacts = contacts_cursor.getCount();
		float num_read = 0;
		if (total_contacts > 0) {
			String id, name;

			ArrayList<String> line = new ArrayList<String>();

			// for each contact
			while (contacts_cursor.moveToNext()) {
				// reset line
				line.clear();

				// contact ID
				id = contacts_cursor.getString(contacts_cursor
						.getColumnIndex(ContactsContract.Contacts._ID));

				// contact name
				name = contacts_cursor
						.getString(contacts_cursor
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

				line.add(name);

				phone_cursor = this.content_resolver.query(Phone.CONTENT_URI,
						null, Phone.CONTACT_ID + " = ?", new String[] { id },
						null);

				while (phone_cursor.moveToNext()) {
					String number = phone_cursor.getString(phone_cursor
							.getColumnIndex(Phone.NUMBER));

					int phone_type = phone_cursor.getInt(phone_cursor
							.getColumnIndex(Phone.TYPE));
					line.add(number);
				}

				email_cursor = this.content_resolver.query(Email.CONTENT_URI,
						null, Email.CONTACT_ID + " = ?", new String[] { id },
						null);

				// all email addresses for the contact
				while (email_cursor.moveToNext()) {

					String email_addr = email_cursor.getString(email_cursor
							.getColumnIndex(Email.DATA));

					int email_type = Integer
							.parseInt(email_cursor.getString(email_cursor
									.getColumnIndex(Email.TYPE)));

					switch (email_type) {
					case (Email.TYPE_HOME):
						break;
					default:
						break;

					}

					line.add(email_addr);
				}

				try {

					// write line to each file

					fos_csv.write(new String(TextUtils.join(",", line) + '\n')
							.getBytes());
					fos_txt.write(new String(TextUtils.join("\t", line) + '\n')
							.getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}

				num_read++;

				this.publishProgress((float) (num_read / total_contacts));

			}
			phone_cursor.close();
			email_cursor.close();
		}
		// There were no contacts to read.
		else {

		}

		try {
			fos_csv.close();
			fos_txt.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	protected void onProgressUpdate(Float... progress) {
		if (this.activity != null) {
			((MainActivity) activity).updateProgress(progress[0]);
		}
	}

	@Override
	protected void onPostExecute(File[] f) {
		if (this.activity != null) {
			((MainActivity) activity).hideProgressBar();
		}

		((MainActivity) activity).sendEmail();
	}
}
