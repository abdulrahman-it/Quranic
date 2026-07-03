import React, { useState, useMemo } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  FlatList,
  SafeAreaView,
  StatusBar,
  Alert,
} from 'react-native';
import { SemanticLight } from '../../design-system/theme';
import { QuranLastReadCard } from '../../features/quran/components/QuranLastReadCard';
import { SurahListItem } from '../../features/quran/components/SurahListItem';

// High-fidelity Quran Index mock data
interface SurahData {
  id: number;
  nameAr: string;
  nameEn: string;
  type: string; // مكة / المدينة
  verseCount: number;
}

const SHARIF_SURAHS: SurahData[] = [
  { id: 1, nameAr: 'الفَاتِحَة', nameEn: 'Al-Fatihah', type: 'مكة', verseCount: 7 },
  { id: 2, nameAr: 'البَقَرَة', nameEn: 'Al-Baqarah', type: 'المدينة', verseCount: 286 },
  { id: 18, nameAr: 'الكَهْف', nameEn: 'Al-Kahf', type: 'مكة', verseCount: 110 },
  { id: 36, nameAr: 'يَاسِين', nameEn: 'Yasin', type: 'مكة', verseCount: 83 },
  { id: 67, nameAr: 'المُلْك', nameEn: 'Al-Mulk', type: 'مكة', verseCount: 30 },
];

export default function QuranIndexScreen() {
  const [searchQuery, setSearchQuery] = useState('');

  const filteredSurahs = useMemo(() => {
    if (!searchQuery.trim()) return SHARIF_SURAHS;
    const cleanQuery = searchQuery.trim().toLowerCase();
    return SHARIF_SURAHS.filter(
      (s) =>
        s.nameAr.includes(cleanQuery) ||
        s.nameEn.toLowerCase().includes(cleanQuery)
    );
  }, [searchQuery]);

  const handleSurahPress = (surah: SurahData) => {
    Alert.alert(
      `سورة ${surah.nameAr}`,
      `قراءة سورة ${surah.nameAr} (${surah.nameEn}) • عدد الآيات: ${surah.verseCount}`,
      [{ text: 'ابدأ القراءة الآن', style: 'default' }]
    );
  };

  const handleLastReadPress = () => {
    Alert.alert(
      'متابعة القراءة',
      'الانتقال المباشر إلى الآية الكريمة ٢٣ من سورة الكهف الشريفة',
      [{ text: 'متابعة', style: 'default' }]
    );
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="dark-content" backgroundColor={SemanticLight.background} />

      <View style={styles.container}>
        {/* App bar title & Header info */}
        <View style={styles.header}>
          <Text style={styles.pageTitle}>الفِهْرِس الشَّامِل</Text>
          <Text style={styles.subtitle}>سُوَر المَصْحَف الشَّرِيف اليوميَّة</Text>
        </View>

        {/* Elegant luxury search container */}
        <View style={styles.searchContainer}>
          <TextInput
            style={styles.searchInput}
            placeholder="ابحث السورة بالرسم أو الاسم..."
            placeholderTextColor={SemanticLight.textSecondary}
            value={searchQuery}
            onChangeText={setSearchQuery}
            underlineColorAndroid="transparent"
          />
          <Text style={styles.searchIcon}>🔍</Text>
        </View>

        {/* Core Scroll list containing HeaderCard and list items (using FlatList) */}
        <FlatList
          data={filteredSurahs}
          keyExtractor={(item) => item.id.toString()}
          showsVerticalScrollIndicator={false}
          ListHeaderComponent={
            <>
              {/* Conditional rendering of Last Read card only if no search to keep layout tidy */}
              {!searchQuery && (
                <QuranLastReadCard
                  surahName="سُورَة الكَهْف"
                  surahId={18}
                  ayahNumber={23}
                  onPress={handleLastReadPress}
                />
              )}
              <View style={styles.listHeaderTitle}>
                <Text style={styles.listLabel}>عناوين السور الكريمة</Text>
              </View>
            </>
          }
          renderItem={({ item }) => (
            <SurahListItem
              id={item.id}
              nameAr={item.nameAr}
              nameEn={item.nameEn}
              type={item.type}
              verseCount={item.verseCount}
              onPress={() => handleSurahPress(item)}
            />
          )}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>عفوًا، لم يتم العثور على أي سورة مطابقة للبحث</Text>
            </View>
          }
          contentContainerStyle={styles.listContent}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: SemanticLight.background,
  },
  container: {
    flex: 1,
    paddingHorizontal: 24,
    paddingTop: 16,
  },
  header: {
    alignItems: 'flex-end',
    marginBottom: 20,
  },
  pageTitle: {
    fontSize: 22,
    fontWeight: 'bold',
    color: SemanticLight.primary,
    writingDirection: 'rtl',
  },
  subtitle: {
    fontSize: 12,
    color: SemanticLight.textSecondary,
    writingDirection: 'rtl',
    marginTop: 2,
  },
  searchContainer: {
    backgroundColor: SemanticLight.surface,
    borderColor: SemanticLight.border,
    borderWidth: 1,
    borderRadius: 16,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    marginBottom: 20,
    height: 52,
    shadowColor: SemanticLight.shadow,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 1,
    shadowRadius: 6,
    elevation: 1,
  },
  searchInput: {
    flex: 1,
    textAlign: 'right',
    color: SemanticLight.textPrimary,
    fontSize: 14,
    height: '100%',
  },
  searchIcon: {
    fontSize: 16,
    marginLeft: 10,
  },
  listHeaderTitle: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  listLabel: {
    fontSize: 14,
    fontWeight: 'bold',
    color: SemanticLight.textPrimary,
    writingDirection: 'rtl',
  },
  listContent: {
    paddingBottom: 40,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 40,
  },
  emptyText: {
    fontSize: 13,
    color: SemanticLight.textSecondary,
    textAlign: 'center',
  },
});
